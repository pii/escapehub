package com.escapehub.mission.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

import com.escapehub.mission.domain.Mission;
import com.escapehub.mission.domain.MissionProblem;

/**
 * Τα αρχεία αποστολών: από προεπιλογή όλα τα {@code missions/*.yml} του classpath, δηλαδή του
 * {@code src/main/resources/missions/}. Η θέση αλλάζει με το {@code escapehub.missions.location}, π.χ. στους
 * ελέγχους.
 */
@Component
public class MissionFiles {

	/**
	 * Η προεπιλεγμένη θέση των αρχείων.
	 */
	public static final String DEFAULT_LOCATION = "classpath*:missions/*.yml";

	private static final Logger LOG = LoggerFactory.getLogger(MissionFiles.class);

	private static final Comparator<Resource> BY_FILENAME = Comparator.comparing(Resource::getFilename,
			Comparator.nullsFirst(Comparator.naturalOrder()));

	private final String location;

	private final ResourcePatternResolver resources;

	private final MissionReader reader;

	public MissionFiles(@Value("${escapehub.missions.location:" + DEFAULT_LOCATION + "}") String location,
			ResourceLoader resourceLoader, Validator validator) {
		this.location = location;
		this.resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		this.reader = new MissionReader(validator);
	}

	/**
	 * Διαβάζει και επικυρώνει όλα τα αρχεία, ταξινομημένα κατά όνομα αρχείου.
	 *
	 * @throws InvalidMissionException στο πρώτο άκυρο αρχείο, ή αν δύο αρχεία έχουν το ίδιο id
	 */
	public List<Mission> loadAll() {
		List<Mission> missions = new ArrayList<>();
		Map<String, String> sourceById = new HashMap<>();
		for (Resource resource : find()) {
			String source = resource.getDescription();
			Mission mission = read(source, resource);
			String previous = sourceById.putIfAbsent(mission.id(), source);
			if (previous != null) {
				String message = "διπλό id '" + mission.id() + "', το έχει και το " + previous;
				throw new InvalidMissionException(source, List.of(new MissionProblem("id", message)));
			}
			missions.add(mission);
		}
		if (missions.isEmpty()) {
			LOG.warn("Δεν βρέθηκαν αρχεία αποστολών στο {}", location);
		}
		else {
			LOG.info("Φορτώθηκαν {} αποστολές από το {}", missions.size(), location);
		}
		return missions;
	}

	private List<Resource> find() {
		try {
			return Arrays.stream(resources.getResources(location)).sorted(BY_FILENAME).toList();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Δεν διαβάστηκαν τα αρχεία αποστολών από το " + location, ex);
		}
	}

	private Mission read(String source, Resource resource) {
		try (InputStream content = resource.getInputStream()) {
			return reader.read(source, content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Δεν διαβάστηκε το αρχείο αποστολής " + source, ex);
		}
	}

}
