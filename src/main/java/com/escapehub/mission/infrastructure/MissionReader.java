package com.escapehub.mission.infrastructure;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.dataformat.yaml.YAMLMapper;

import com.escapehub.mission.domain.Mission;
import com.escapehub.mission.domain.MissionProblem;
import com.escapehub.mission.domain.MissionValidator;

/**
 * Διαβάζει ένα αρχείο αποστολής σε τρία βήματα. Σε κάθε βήμα, αν βρεθούν προβλήματα, σταματά με
 * {@link InvalidMissionException}:
 * <ol>
 * <li>YAML σε {@link Mission}. Τα άγνωστα πεδία (π.χ. ορθογραφικό λάθος) και τα διπλά κλειδιά είναι λάθη.</li>
 * <li>Bean Validation για τη δομή: υποχρεωτικά πεδία, μορφή των id, θετικοί αριθμοί.</li>
 * <li>{@link MissionValidator} για τις αναφορές, τους τύπους γρίφων, τους κύκλους και την ολοκλήρωση.</li>
 * </ol>
 */
class MissionReader {

	/**
	 * Ένα boolean ή int που λείπει από το αρχείο παίρνει την προεπιλεγμένη τιμή ({@code false}, {@code 0}). Το
	 * Jackson 3 τα θεωρεί αλλιώς {@code null} και απορρίπτει το αρχείο. Ένα υποχρεωτικό int που λείπει (π.χ.
	 * {@code version}) το πιάνει το {@code @Positive}.
	 */
	private final ObjectMapper yaml = YAMLMapper.builder()
		.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
		.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
		.build();

	private final Validator validator;

	MissionReader(Validator validator) {
		this.validator = validator;
	}

	/**
	 * Διαβάζει και επικυρώνει μια αποστολή.
	 *
	 * @param source το όνομα του αρχείου για τα μηνύματα
	 * @throws InvalidMissionException αν το αρχείο είναι άκυρο
	 */
	Mission read(String source, InputStream content) {
		Mission mission = parse(source, content);
		List<MissionProblem> problems = constraintViolations(mission);
		if (problems.isEmpty()) {
			problems = MissionValidator.validate(mission);
		}
		if (!problems.isEmpty()) {
			throw new InvalidMissionException(source, problems);
		}
		return mission;
	}

	private Mission parse(String source, InputStream content) {
		Mission mission;
		try {
			mission = yaml.readValue(content, Mission.class);
		}
		catch (JacksonException ex) {
			throw invalid(source, new MissionProblem(field(ex), describe(ex)));
		}
		if (mission == null) {
			throw invalid(source, new MissionProblem("", "το αρχείο είναι κενό"));
		}
		return mission;
	}

	private static InvalidMissionException invalid(String source, MissionProblem problem) {
		return new InvalidMissionException(source, List.of(problem));
	}

	private List<MissionProblem> constraintViolations(Mission mission) {
		return validator.validate(mission)
			.stream()
			.map(violation -> new MissionProblem(field(violation), violation.getMessage()))
			.sorted(Comparator.comparing(MissionProblem::field))
			.toList();
	}

	/**
	 * Το πεδίο όπως στο YAML, π.χ. {@code puzzles[1].unlockedBy[0]}. Αφαιρεί κόμβους του Bean Validation όπως το
	 * {@code <list element>}.
	 */
	private static String field(ConstraintViolation<Mission> violation) {
		return violation.getPropertyPath().toString().replaceAll("\\.?<[a-z ]+>", "");
	}

	private static String field(JacksonException ex) {
		return ex.getPath()
			.stream()
			.map(reference -> (reference.getPropertyName() != null) ? "." + reference.getPropertyName()
					: "[" + reference.getIndex() + "]")
			.collect(Collectors.joining())
			.replaceFirst("^\\.", "");
	}

	private static String describe(JacksonException ex) {
		String message;
		if (ex instanceof UnrecognizedPropertyException unknown) {
			message = "άγνωστο πεδίο '" + unknown.getPropertyName() + "'. Επιτρεπτά πεδία: "
					+ unknown.getKnownPropertyIds();
		}
		else if (ex instanceof InvalidFormatException invalid && invalid.getTargetType() != null
				&& invalid.getTargetType().isEnum()) {
			message = "άγνωστη τιμή '" + invalid.getValue() + "'. Επιτρεπτές τιμές: "
					+ List.of(invalid.getTargetType().getEnumConstants());
		}
		else {
			message = ex.getOriginalMessage();
		}
		TokenStreamLocation location = ex.getLocation();
		if (location == null || location.getLineNr() <= 0) {
			return message;
		}
		return message + " (γραμμή " + location.getLineNr() + ")";
	}

}
