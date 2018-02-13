package zemberek.morphology.morphotactics;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.analyzer.MorphemeSurfaceForm.SuffixTemplateToken;
import zemberek.morphology.analyzer.MorphemeSurfaceForm.SuffixTemplateTokenizer;
import zemberek.morphology.analyzer.SearchPath;
import zemberek.morphology.structure.Turkish;

public class SuffixTransition extends MorphemeTransition {

  // this string represents the possible surface forms for this transition.
  public final String surfaceTemplate;

  private List<SuffixTemplateToken> tokenList;

  Condition condition;

  private SuffixTransition(Builder builder) {
    Preconditions.checkNotNull(builder.from);
    Preconditions.checkNotNull(builder.to);
    this.from = builder.from;
    this.to = builder.to;
    this.surfaceTemplate = builder.surfaceTemplate == null ? "" : builder.surfaceTemplate;
    this.condition = builder.condition;
    conditionsFromTemplate(this.surfaceTemplate);
    this.tokenList = Lists
        .newArrayList(new SuffixTemplateTokenizer(this.surfaceTemplate));

  }

  public boolean canPass(SearchPath path) {
    if (condition == null) {
      return true;
    }
    return condition.check(path);
  }

  private void connect() {
    from.addOutgoing(this);
    to.addIncoming(this);
  }

  // adds vowel-consonant expectation related conditions automatically.
  // TODO: consider moving this to morphotactics somehow.
  private void conditionsFromTemplate(String template) {
    if (template == null || template.length() == 0) {
      return;
    }
    String lower = template.toLowerCase(Turkish.LOCALE);
    Condition c = null;
    if (template.startsWith(">") || !TurkishAlphabet.INSTANCE.isVowel(lower.charAt(0))) {
      c = Conditions.contains(PhoneticAttribute.ExpectsVowel).not();
    }
    if (template.startsWith("+") || TurkishAlphabet.INSTANCE.isVowel(lower.charAt(0))) {
      c = Conditions.contains(PhoneticAttribute.ExpectsConsonant).not();
    }
    if (c != null) {
      if (condition == null) {
        condition = c;
      } else {
        condition = condition.and(c);
      }
    }
  }

  public Builder builder() {
    return new Builder();
  }

  public String toString() {
    return "[" + from.id + "→" + to.id +
        (surfaceTemplate.isEmpty() ? "" : (":" + surfaceTemplate))
        + "]";
  }

  public static class Builder {

    MorphemeState from;
    MorphemeState to;
    String surfaceTemplate;
    Condition condition;

    public Builder from(MorphemeState from) {
      checkIfDefined(this.from, "from");
      this.from = from;
      return this;
    }

    private void checkIfDefined(Object o, String name) {
      Preconditions.checkArgument(
          o == null,
          "[%s = %s] is already defined.", name, o);
    }

    public Builder to(MorphemeState to) {
      checkIfDefined(this.to, "to");
      this.to = to;
      return this;
    }

    public Builder setCondition(Condition _condition) {
      if (condition != null) {
        Log.warn("Condition was already set.");
      }
      this.condition = _condition;
      return this;
    }

    public Builder empty() {
      return surfaceTemplate("");
    }

    public Builder surfaceTemplate(String template) {
      checkIfDefined(this.surfaceTemplate, "surfaceTemplate");
      this.surfaceTemplate = template;
      return this;
    }

    // generates a transition and connects it.
    public SuffixTransition build() {
      SuffixTransition transition = new SuffixTransition(this);
      transition.connect();
      return transition;
    }

    // generates a transition and connects it.
    public MorphemeState add() {
      SuffixTransition transition = new SuffixTransition(this);
      transition.connect();
      return transition.from;
    }
  }

  public List<SuffixTemplateToken> getTokenList() {
    return tokenList;
  }

  public boolean hasSurfaceForm() {
    return tokenList.size() > 0;
  }

  public SuffixTemplateToken getLastTemplateToken() {
    if (tokenList.size() == 0) {
      return null;
    } else {
      return tokenList.get(tokenList.size() - 1);
    }
  }

}
