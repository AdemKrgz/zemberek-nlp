package zemberek.morphology.morphotactics;

import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.analyzer.SearchPath;
import zemberek.morphology.lexicon.DictionaryItem;

public class Rules {

  public static Rule allowOnly(RootAttribute attribute) {
    return new AllowOnlyRootAttribute(attribute);
  }

  public static Rule allowOnly(PhoneticAttribute attribute) {
    return new AllowOnlyIfContainsPhoneticAttribute(attribute);
  }

  public static Rule allowOnly(DictionaryItem item) {
    return new AllowDictionaryItem(item);
  }

  public static Rule rejectIfContains(PhoneticAttribute attribute) {
    return new RejectIfContainsPhoneticAttribute(attribute);
  }

  public static Rule rejectIfContains(RootAttribute attribute) {
    return new RejectIfContainsRootAttribute(attribute);
  }

  public static Rule rejectIfContains(DictionaryItem item) {
    return new RejectDictionaryItem(item);
  }

  private static class AllowOnlyRootAttribute implements Rule {

    RootAttribute attribute;

    AllowOnlyRootAttribute(RootAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean check(SearchPath visitor) {
      // normally this should also check if visitor has no derivation.
      return visitor.containsRootAttribute(attribute);
    }

    @Override
    public String toString() {
      return "AllowOnlyRootAttribute{" + attribute + '}';
    }
  }

  private static class RejectIfContainsRootAttribute implements Rule {

    RootAttribute attribute;

    RejectIfContainsRootAttribute(RootAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean check(SearchPath visitor) {
      // normally this should also check if visitor has no derivation.
      return !visitor.containsRootAttribute(attribute);
    }

    @Override
    public String toString() {
      return "RejectIfContainsRootAttribute{" + attribute + '}';
    }
  }


  private static class AllowOnlyIfContainsPhoneticAttribute implements Rule {

    PhoneticAttribute attribute;

    public AllowOnlyIfContainsPhoneticAttribute(PhoneticAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean check(SearchPath visitor) {
      return visitor.getPhoneticAttributes().contains(attribute);
    }

    @Override
    public String toString() {
      return "AllowOnlyIfContainsPhoneticAttribute{" + attribute + '}';
    }
  }

  private static class RejectIfContainsPhoneticAttribute implements Rule {

    PhoneticAttribute attribute;

    RejectIfContainsPhoneticAttribute(PhoneticAttribute attribute) {
      this.attribute = attribute;
    }

    @Override
    public boolean check(SearchPath visitor) {
      return !visitor.getPhoneticAttributes().contains(attribute);
    }

    @Override
    public String toString() {
      return "RejectIfContainsPhoneticAttribute{" + attribute + '}';
    }
  }

  private static class AllowDictionaryItem implements Rule {

    DictionaryItem item;

    AllowDictionaryItem(DictionaryItem item) {
      this.item = item;
    }

    @Override
    public boolean check(SearchPath visitor) {
      // normally this should also check if visitor has no derivation.
      return item != null && visitor.hasDictionaryItem(item);
    }

    @Override
    public String toString() {
      return "AllowDictionaryItem{" + item + '}';
    }
  }

  private static class RejectDictionaryItem implements Rule {

    DictionaryItem item;

    RejectDictionaryItem(DictionaryItem item) {
      this.item = item;
    }

    @Override
    public boolean check(SearchPath visitor) {
      // normally this should also check if visitor has no derivation.
      return item == null || !visitor.hasDictionaryItem(item);
    }

    @Override
    public String toString() {
      return "RejectDictionaryItem{" + item + '}';
    }
  }

  public static class RejectIfHasAnySuffixSurface implements Rule {

    @Override
    public boolean check(SearchPath visitor) {
      return !visitor.containsSuffixWithSurface();
    }

    @Override
    public String toString() {
      return "RejectIfHasAnySuffixSurface{}";
    }
  }

}
