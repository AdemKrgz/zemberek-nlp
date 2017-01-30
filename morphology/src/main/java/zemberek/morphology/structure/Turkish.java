package zemberek.morphology.structure;

import zemberek.core.collections.UIntMap;
import zemberek.core.io.KeyValueReader;
import zemberek.core.logging.Log;
import zemberek.core.turkish.TurkishAlphabet;

import java.io.IOException;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains some Turkish language specific helper methods and properties.
 */
public class Turkish {
    public static final Locale LOCALE = new Locale("tr");
    public static final TurkishAlphabet Alphabet = new TurkishAlphabet();
    public static final Collator COLLATOR = Collator.getInstance(LOCALE);

    static UIntMap<String> turkishLetterProns  = new UIntMap<>();

    static {
        try {
            Map<String, String> map = new KeyValueReader("=", "##")
                    .loadFromStream(
                            Turkish.class.getResourceAsStream("/tr/phonetics/turkish-letter-pronunciation.txt"), "utf-8");
            for (String s : map.keySet()) {
                if (s.length() != 1) {
                    Log.warn("1 Character keys are expected. But it is : %s", s);
                }
                turkishLetterProns.put(s.charAt(0), map.get(s));
            }

        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String inferPronunciation(String w) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (turkishLetterProns.containsKey(c)) {
                sb.append(turkishLetterProns.get(c));
            }
            else {
                Log.warn("Cannot identify character " + String.valueOf(c) + " in pronunciation of :[" + w + "]");
            }
        }
        return sb.toString();
    }

    public static String capitalize(String word) {
        if (word.length() == 0)
            return word;
        return word.substring(0, 1).toUpperCase(LOCALE) + word.substring(1).toLowerCase(LOCALE);
    }

    private static class TurkishStringComparator implements Comparator<String> {
        public int compare(String o1, String o2) {
            return COLLATOR.compare(o1, o2);
        }
    }

    public static final Comparator<String> STRING_COMPARATOR_ASC = new TurkishStringComparator();

    static Pattern WORD_BEGIN_END_SEPARATOR =
            Pattern.compile("^([.,'\"()\\[\\]{}:;*$]+|)(.+?)([:;)(\\[\\]{}'?!,.\"\\-*$]+|)$");

    /**
     * TODO: should not separate dot symbols from numbers.
     * <p>separates begin-end symbols from words. there are some exceptions tough, it does not separate +- from beginning
     * <p>examples:
     * <p>'123 -> ' 123
     * <p>+123 ->  +123
     * <p>"123" -> " 123 "
     * <p>,23,2 -> , 23,2
     * <p>merhaba? -> merhaba ?
     * <p>merhaba. -> merhaba.
     *
     * @param input input string.
     * @return output.
     */
    public static String separateBeginEndSymbolsFromWord(String input) {
        Matcher matcher = WORD_BEGIN_END_SEPARATOR.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length() + 3);
        sb.append(matcher.group(1))
                .append(" ")
                .append(matcher.group(2))
                .append(" ")
                .append(matcher.group(3));
        return sb.toString().trim();
    }

    /**
     * This method converts different single and double quote symbols to a unified form.
     * also it reduces two connected single quotes to a one double quote.
     *
     * @param input input string.
     * @return cleaned input string.
     */
    public static String normalizeQuotesHyphens(String input) {
        // rdquo, ldquo, laquo, raquo, Prime sybols in unicode.
        return input
                .replaceAll("[\u201C\u201D\u00BB\u00AB\u2033\u0093\u0094]|''", "\"")
                .replaceAll("[\u0091\u0092\u2032´`’‘]", "'")
                .replaceAll("[\u0096\u0097–]", "-");
    }

    public static String normalizeCircumflex(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        input = input.toLowerCase(LOCALE);
        for (char c : input.toCharArray()) {
            switch (c) {
                case 'Â':
                    sb.append("A");
                    break;
                case 'â':
                    sb.append("a");
                    break;
                case 'Î':
                    sb.append("İ");
                    break;
                case 'î':
                    sb.append("i");
                    break;
                case 'Û':
                    sb.append("U");
                    break;
                case 'û':
                    sb.append("u");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String toAscii(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case 'ç':
                    sb.append('c');
                    break;
                case 'ğ':
                    sb.append('g');
                    break;
                case 'ı':
                    sb.append('i');
                    break;
                case 'ö':
                    sb.append('o');
                    break;
                case 'ş':
                    sb.append('s');
                    break;
                case 'ü':
                    sb.append('u');
                    break;
                case 'Ç':
                    sb.append('C');
                    break;
                case 'Ğ':
                    sb.append('G');
                    break;
                case 'İ':
                    sb.append('I');
                    break;
                case 'Ö':
                    sb.append('O');
                    break;
                case 'Ş':
                    sb.append('S');
                    break;
                case 'Ü':
                    sb.append('U');
                    break;
                case TurkishAlphabet.a_CIRC:
                    sb.append('a');
                    break;
                case TurkishAlphabet.A_CIRC:
                    sb.append('A');
                    break;
                case TurkishAlphabet.i_CIRC:
                    sb.append('i');
                    break;
                case TurkishAlphabet.I_CIRC:
                    sb.append('İ');
                    break;
                case TurkishAlphabet.u_CIRC:
                    sb.append('u');
                    break;
                case TurkishAlphabet.U_CIRC:
                    sb.append('U');
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
