package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.util.Log;
import android.util.Pair;


import org.thoughtcrime.securesms.R;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BasicEnglish {
  private static final String TAG = BasicEnglish.class.getSimpleName();
  Context           context;
  byte[]            input;
  List<String> n, vi, vt, adj, adv, p, art;

  public BasicEnglish(Context context) {
    this.context = context;
    try {
      n   = readFile(R.raw.basic_nouns);
      vi  = readFile(R.raw.basic_verbs_i);
      vt  = readFile(R.raw.basic_verbs_t);
      adj = readFile(R.raw.basic_adjectives);
      adv = readFile(R.raw.basic_adverbs);
      p   = readFile(R.raw.basic_prepositions);
      art = readFile(R.raw.basic_articles);
    } catch (IOException e) {
      Log.w(TAG, e);
    }
  }

  List<String> readFile(int resId) throws IOException {
    if (context.getApplicationContext() == null) throw new AssertionError("app context can't be null");
    BufferedReader in = new BufferedReader(new InputStreamReader(
        context.getApplicationContext()
               .getResources()
               .openRawResource(resId)));
    List<String> words = new ArrayList<String>();
    String word = in.readLine();
    while (word != null) {
      words.add(word);
      word = in.readLine();
    }
    in.close();
    return words;
  }

  public String fromBytes(final byte[] bytes, int desiredBytes) throws IOException {
    BitInputStream bin         = new BitInputStream(new ByteArrayInputStream(bytes));
    EntropyString  fingerprint = new EntropyString();
    int bitsRead = 0;
    while (fingerprint.getBits() < (desiredBytes * 8)) {
      try {
        fingerprint.append(getSentence(bin));
        fingerprint.append("\n");
      } catch (IOException e) {
        Log.e(TAG, "hit IOException when creating the mnemonic sentence");
        throw e;
      }
    }
    return fingerprint.toString();
  }

  /**
   * Grab a word for a list of them using the necessary bits to choose from a BitInputStream
   * @param words the list of words to select from
   * @param bin the bit input stream to encode from
   * @return A Pair of the word and the number of bits consumed from the stream
   */
  private EntropyString getWord(List<String> words, BitInputStream bin) throws IOException {
    final int neededBits = log(words.size(), 2);
    Log.w(TAG, "need " + neededBits + " bits of entropy");
    int bits = bin.readBits(neededBits);
    Log.w(TAG, "got word " + words.get(bits) + " with " + neededBits + " bits of entropy");
    return new EntropyString(words.get(bits), neededBits);
  }

  private EntropyString getNounPhrase(BitInputStream bits) throws IOException {
    final EntropyString phrase = new EntropyString();
    phrase.append(getWord(art, bits)).append(" ");
    if (bits.readBit() != 0) {
      phrase.append(getWord(adj, bits)).append(" ");
    }
    phrase.incBits();

    phrase.append(getWord(n, bits));
    Log.w(TAG, "got phrase " + phrase + " with " + phrase.getBits() + " bits of entropy");
    return phrase;
  }

  EntropyString getSentence(BitInputStream bits) throws IOException {
    final EntropyString sentence = new EntropyString();
    sentence.append(getNounPhrase(bits)).append(" ");   // Subject
    if (bits.readBit() != 0) {
      sentence.append(getWord(vt, bits)).append(" ");   // Transitive verb
      sentence.append(getNounPhrase(bits)).append(" "); // Object of transitive verb
    } else {
      sentence.append(getWord(vi, bits)).append(" ");   // Intransitive verb
    }
    sentence.incBits();

    if (bits.readBit() != 0) sentence.append(getWord(adv, bits)).append(" "); // Adverb
    sentence.incBits();
    if (bits.readBit() != 0) {
      sentence.append(getWord(p, bits));    // Preposition
    }
    sentence.incBits();
    Log.w(TAG, "got sentence " + sentence + " with " + sentence.getBits() + " bits of entropy");
    return sentence;
  }

  public static class EntropyString {
    private StringBuilder builder;
    private int bits;

    public EntropyString(String phrase, int bits) {
      this.builder = new StringBuilder(phrase);
      this.bits = bits;
    }

    public EntropyString() {
      this("", 0);
    }

    public EntropyString append(EntropyString phrase) {
      builder.append(phrase);
      bits += phrase.getBits();
      return this;
    }

    public EntropyString append(String string) {
      builder.append(string);
      return this;
    }

    public int getBits() {
      return bits;
    }

    public void setBits(int bits) {
      this.bits = bits;
    }

    public void incBits() {
      bits += 1;
    }

    @Override
    public String toString() {
      return builder.toString();
    }
  }

  private static int log(int x, int base)
  {
    return (int) (Math.log(x) / Math.log(base));
  }
}