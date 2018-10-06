package zemberek.normalization;

import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.text.TextUtil;
import zemberek.tokenization.TurkishSentenceExtractor;

class TextCleaner {

  static List<String> cleanAndExtractSentences(List<String> input) {
    List<String> lines = input.stream()
        .filter(s -> !s.startsWith("<"))
        .map(TextUtil::normalizeSpacesAndSoftHyphens)
        .collect(Collectors.toList());
    return TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines);
  }
}
