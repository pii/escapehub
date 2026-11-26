package com.escapehub.mission.domain;

/**
 * Οι τύποι γρίφων (ADR-0001). Είναι κλειστό σύνολο: ένας νέος τύπος χρειάζεται κώδικα, ενώ μια νέα αποστολή με
 * τους υπάρχοντες τύπους χρειάζεται μόνο αρχείο (MVP-9).
 */
public enum PuzzleType {

	/**
	 * Ελεύθερη απάντηση, που συγκρίνεται με τις αποδεκτές τιμές ({@code answer.accepted}).
	 */
	TEXT_ANSWER,

	/**
	 * Ενέργειες ({@code actions}) με συγκεκριμένη σειρά ({@code answer.steps}).
	 */
	SEQUENCE,

	/**
	 * Απάντηση ({@code answer.accepted}) από τουλάχιστον {@code contributors} διαφορετικούς παίκτες.
	 */
	COOPERATIVE,

	/**
	 * Επιλογή στοιχείων από το ημερολόγιο ({@code answer.evidence}) ή συμπλήρωση πεδίων ({@code fields},
	 * {@code answer.values}).
	 */
	COMBINATION

}
