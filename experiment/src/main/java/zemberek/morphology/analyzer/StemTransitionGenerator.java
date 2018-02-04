package zemberek.morphology.analyzer;

import static zemberek.core.turkish.RootAttribute.CompoundP3sg;
import static zemberek.core.turkish.RootAttribute.CompoundP3sgRoot;
import static zemberek.core.turkish.RootAttribute.Doubling;
import static zemberek.core.turkish.RootAttribute.InverseHarmony;
import static zemberek.core.turkish.RootAttribute.LastVowelDrop;
import static zemberek.core.turkish.RootAttribute.ProgressiveVowelDrop;
import static zemberek.core.turkish.RootAttribute.Special;
import static zemberek.core.turkish.RootAttribute.Voicing;

import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.TurkicLetter;
import zemberek.core.turkish.TurkishLetterSequence;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.LexiconException;
import zemberek.morphology.morphotactics.StemTransition;
import zemberek.morphology.morphotactics.TurkishMorphotactics;


/**
 * This class generates StemNode objects from Dictionary Items. Generated Nodes are not connected.
 */
public class StemTransitionGenerator {

  TurkishAlphabet alphabet = TurkishAlphabet.INSTANCE;

  TurkishMorphotactics morphotactics;

  EnumSet<RootAttribute> modifiers = EnumSet.of(
      Doubling,
      LastVowelDrop,
      ProgressiveVowelDrop,
      InverseHarmony,
      Voicing,
      Special,
      CompoundP3sg,
      CompoundP3sgRoot
  );

  public StemTransitionGenerator(TurkishMorphotactics morphotactics) {
    this.morphotactics = morphotactics;
  }

  /**
   * Generates StemTransition objects from the dictionary item. <p>Most of the time a single
   * StemNode is generated.
   *
   * @param item DictionaryItem
   * @return one or more StemTransition objects.
   */
  public List<StemTransition> generate(DictionaryItem item) {
    if (hasModifierAttribute(item)) {
      return generateModifiedRootNodes(item);
    } else {
      EnumSet<PhoneticAttribute> phoneticAttributes = calculateAttributes(item.pronunciation);
      StemTransition transition = new StemTransition(
          item.root,
          item,
          phoneticAttributes,
          EnumSet.noneOf(PhoneticExpectation.class),
          morphotactics.getRootState(item)
      );

      return Lists.newArrayList(transition);
    }
  }


  private boolean hasModifierAttribute(DictionaryItem item) {
    for (RootAttribute attr : modifiers) {
      if (item.attributes.contains(attr)) {
        return true;
      }
    }
    return false;
  }

  private EnumSet<PhoneticAttribute> calculateAttributes(String input) {
    return calculateAttributes(new TurkishLetterSequence(input, alphabet));
  }

  private EnumSet<PhoneticAttribute> calculateAttributes(TurkishLetterSequence sequence) {
    EnumSet<PhoneticAttribute> attrs = EnumSet.noneOf(PhoneticAttribute.class);
    // general phonetic attributes.
    if (sequence.vowelCount() > 0) {
      if (sequence.lastVowel().isRounded()) {
        attrs.add(PhoneticAttribute.LastVowelRounded);
      } else {
        attrs.add(PhoneticAttribute.LastVowelUnrounded);
      }
      if (sequence.lastVowel().isFrontal()) {
        attrs.add(PhoneticAttribute.LastVowelFrontal);
      } else {
        attrs.add(PhoneticAttribute.LastVowelBack);
      }
    }
    if (sequence.lastLetter().isVowel()) {
      // elma
      attrs.add(PhoneticAttribute.LastLetterVowel);
    } else {
      attrs.add(PhoneticAttribute.LastLetterConsonant);
    }
    if (sequence.lastLetter().isVoiceless()) {
      attrs.add(PhoneticAttribute.LastLetterVoiceless);
      if (sequence.lastLetter().isStopConsonant()) {
        // kitap
        attrs.add(PhoneticAttribute.LastLetterVoicelessStop);
      }
    } else {
      attrs.add(PhoneticAttribute.LastLetterVoiced);
    }
    return attrs;
  }

  private List<StemTransition> generateModifiedRootNodes(DictionaryItem dicItem) {

    if (dicItem.hasAttribute(RootAttribute.Special)) {
      // TODO: handleSpecialStems like ben demek etc.
    }

    TurkishLetterSequence modifiedSeq = new TurkishLetterSequence(dicItem.pronunciation, alphabet);
    EnumSet<PhoneticAttribute> originalAttrs = calculateAttributes(dicItem.pronunciation);
    EnumSet<PhoneticAttribute> modifiedAttrs = originalAttrs.clone();
    EnumSet<PhoneticExpectation> originalExpectations = EnumSet.noneOf(PhoneticExpectation.class);
    EnumSet<PhoneticExpectation> modifiedExpectations = EnumSet.noneOf(PhoneticExpectation.class);

    for (RootAttribute attribute : dicItem.attributes) {

      // generate other boundary attributes and modified root state.
      switch (attribute) {
        case Voicing:
          TurkicLetter last = modifiedSeq.lastLetter();
          TurkicLetter modifiedLetter = alphabet.voice(last);
          if (modifiedLetter == null) {
            throw new LexiconException("Voicing letter is not proper in:" + dicItem);
          }
          if (dicItem.lemma.endsWith("nk")) {
            modifiedLetter = TurkishAlphabet.L_g;
          }
          modifiedSeq.changeLetter(modifiedSeq.length() - 1, modifiedLetter);
          modifiedAttrs.remove(PhoneticAttribute.LastLetterVoicelessStop);
          originalExpectations.add(PhoneticExpectation.ConsonantStart);
          modifiedExpectations.add(PhoneticExpectation.VowelStart);
          break;
        case Doubling:
          modifiedSeq.append(modifiedSeq.lastLetter());
          originalExpectations.add(PhoneticExpectation.ConsonantStart);
          modifiedExpectations.add(PhoneticExpectation.VowelStart);
          break;
        case LastVowelDrop:
          if (modifiedSeq.lastLetter().isVowel()) {
            modifiedSeq.delete(modifiedSeq.length() - 1);
            modifiedExpectations.add(PhoneticExpectation.ConsonantStart);
          } else {
            modifiedSeq.delete(modifiedSeq.length() - 2);
            if (!dicItem.primaryPos.equals(PrimaryPos.Verb)) {
              originalExpectations.add(PhoneticExpectation.ConsonantStart);
            }
            modifiedExpectations.add(PhoneticExpectation.VowelStart);
          }
          break;
        case InverseHarmony:
          originalAttrs.add(PhoneticAttribute.LastVowelFrontal);
          originalAttrs.remove(PhoneticAttribute.LastVowelBack);
          modifiedAttrs.add(PhoneticAttribute.LastVowelFrontal);
          modifiedAttrs.remove(PhoneticAttribute.LastVowelBack);
          break;
        case ProgressiveVowelDrop:
          modifiedSeq.delete(modifiedSeq.length() - 1);
          if (modifiedSeq.hasVowel()) {
            modifiedAttrs = calculateAttributes(modifiedSeq);
          }
          break;
        default:
          break;
      }
    }

    StemTransition original = new StemTransition(
        dicItem.root,
        dicItem,
        originalAttrs,
        originalExpectations,
        morphotactics.getRootState(dicItem));

    StemTransition modified = new StemTransition(
        modifiedSeq.toString(),
        dicItem,
        modifiedAttrs,
        modifiedExpectations,
        morphotactics.getRootState(dicItem));
    //TODO: if both are equal, return only one.

    return Lists.newArrayList(original, modified);
  }

}