package de.masterarbeit.cisco;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Einfacher Parser für Cisco Konfigurationsdateien (show running config). Idee: https://github.com/mpenning/ciscoconfparse
 * 
 * Block = Ein Konfigurationsblock in der Cisco running config. Ein Block beginnt bei einer uneingerückten Zeile und alle
 * anschließenden eingerückten Zeilen gehören dazu.
 * 
 * Parent = Uneingerückte Line in einem Block (also immer die erste Zeile).
 * 
 * Children = Eingerückte Linie in einem Block (s.o.), Kinder vom Parent.
 * 
 * @author Marcel Schuster
 */
public class JCiscoConfParse {
	private List<List<String>> runningConfig = new ArrayList<>();

	/**
	 * Verarbeitet die angegebene Konfigurationsdatei zeilenweise und speichert das Resultat als Liste von Blocks (s.o.).
	 * 
	 * @param path
	 *            Pfad zur Konfigurationsdatei, welche eingelesen werden soll
	 * @throws IOException
	 *             Stumpfe Weiterleitung der Exception
	 */
	public JCiscoConfParse(String path) throws IOException {
		List<String> config = Files.readAllLines(Paths.get(path), Charset.defaultCharset());
		List<String> block = new ArrayList<>();

		for (String line : config) {
			if (line.isEmpty() || line.startsWith("!")) {
				continue;
			} else if (line.startsWith("  ") || line.startsWith(" ")) {
				// Zum letzten Block hinzufügen, welcher über "block" noch referenziert sein sollte.
				block.add(line);
			} else {
				block = new ArrayList<>();
				block.add(line);
				runningConfig.add(block);
			}
		}
	}

	/**
	 * Filtert Blöcke nach dem Inhalt der ersten Zeile (aka. Parent).
	 * 
	 * @param pattern
	 *            Regex-Pattern, welches auf den Parent angewendet wird.
	 * @return Alle Blöcke, die ein Match mit dem Pattern und dem Parent haben
	 */
	public List<List<String>> findParents(String pattern) {
		List<List<String>> result = new ArrayList<>();

		for (List<String> block : runningConfig) {
			// Im Inhalt der ersten Zeile (= Parent) ein Match suchen
			if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(block.get(0)).find()) {
				result.add(block);
			}
		}

		return result;
	}
}
