package de.masterarbeit.cisco;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Einfacher Parser für Cisco CDP-Informationen (show cdp neighbors). Die Verarbeitung findet ähnlich zu JCiscoConfParse in
 * Blöcken statt, da die nötigen Informationen entweder einzeilig oder zweizeilig sind.
 * 
 * @author Marcel Schuster
 *
 */
public class JCiscoCDPParse {
	private List<List<String>> cdpResult = new ArrayList<>();

	/**
	 * Verarbeitet die angegebene CDP-Datei zeilenweise und speichert das Resultat als Liste von Blöcken.
	 * 
	 * @param path
	 *            Pfad zur CDP-Datei, welche eingelesen werden soll
	 * @throws IOException
	 *             Stumpfe Weiterleitung der Exception
	 */
	public JCiscoCDPParse(String path) throws IOException {
		List<String> config = Files.readAllLines(Paths.get(path), Charset.defaultCharset());
		List<String> block = new ArrayList<>();
		Boolean start = false;

		for (String line : config) {
			if (!start) {
				// Entweder "Device-ID" oder "Device ID"
				if (Pattern.matches("^Device(-| )ID[\\s]+(.*)", line)) {
					start = true;
				}
				continue;
			} else if (line.isEmpty()) {
				continue;
			} else if (Pattern.matches("^[\\s]+(.*)", line)) {
				// Zum letzten Block hinzufügen, welcher über "block" noch referenziert sein sollte.
				block.add(line);
			} else {
				block = new ArrayList<>();
				block.add(line);
				cdpResult.add(block);
			}
		}
	}

	/**
	 * Sucht den angegebenen Namen des Nachbarn in den CDP-Blöcken.
	 * 
	 * @param neighbor
	 *            Nachbar, nach welchem in den Blöcken gesucht werden soll
	 * @return Zeilen der CDP-Datei, die den gesuchten Nachbarn enthalten
	 */
	public List<List<String>> findNeighbors(String neighbor) {
		List<List<String>> result = new ArrayList<>();

		for (List<String> block : cdpResult) {
			// Im Inhalt der ersten Zeile filtern
			if (Pattern.compile(neighbor, Pattern.CASE_INSENSITIVE).matcher(block.get(0)).find()) {
				result.add(block);
			}
		}

		return result;
	}
}
