package de.masterarbeit.toolbox;

/**
 * Einfache Klasse zur Vorberechnung der Lookup-Table für OCL-konforme (aber extrem umständliche) Berechnung des binären UND.
 * 
 * @author Marcel Schuster
 *
 */
public class IPCalculator {
	public static void main(String[] args) {
		int counter = 0;

		// Läuft von 10000000 bis 11111111 (Nuller müssen nicht betrachtet werden)
		for (int i = 7, number = 1 << 7; i > 0; i--, number |= 1 << i) {
			for (int j = 1; j < 255; j++) {
				if (number == j)
					continue;
				System.out.print("Sequence{" + number + "," + j + "," + (number & j) + "}, ");
				counter++;
			}
			System.out.println();
		}

		System.out.println();
		System.out.println("Precalculations: " + counter);
	}
}
