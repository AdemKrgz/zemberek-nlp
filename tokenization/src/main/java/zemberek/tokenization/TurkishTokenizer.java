package zemberek.tokenization;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import zemberek.tokenization.antlr.TurkishLexer;


/**
 * A wrapper for Antlr generated lexer.
 */
public class TurkishTokenizer {

  public static final TurkishTokenizer ALL = builder().acceptAll().build();
  private static final int MAX_TOKEN_TYPE = TurkishLexer.VOCABULARY.getMaxTokenType();
  public static final TurkishTokenizer DEFAULT = builder()
      .acceptAll()
      .ignoreTypes(TurkishLexer.NewLine, TurkishLexer.SpaceTab)
      .build();

  private static final BaseErrorListener IGNORING_ERROR_LISTENER = new ConsoleErrorListener();

  private long acceptedTypeBits;

  private TurkishTokenizer(long acceptedTypeBits) {
    this.acceptedTypeBits = acceptedTypeBits;
  }

  public static Builder builder() {
    return new Builder();
  }

  private static void validateType(int i) {
    if (i < 0) {
      throw new IllegalStateException("Token index cannot be negative. But it is : " + i);
    }

    if (i > MAX_TOKEN_TYPE) {
      throw new IllegalStateException("Token index cannot be larger than " + MAX_TOKEN_TYPE
          + ". But it is : " + i);
    }
  }

  private static TurkishLexer lexerInstance(CharStream inputStream) {
    TurkishLexer lexer = new TurkishLexer(inputStream);
    lexer.removeErrorListeners();
    lexer.addErrorListener(IGNORING_ERROR_LISTENER);
    return lexer;
  }

  public boolean isTypeAccepted(int i) {
    validateType(i);
    return !typeAccepted(i);
  }

  public boolean isTypeIgnored(int i) {
    validateType(i);
    return !typeAccepted(i);
  }

  private boolean typeAccepted(int i) {
    return (acceptedTypeBits & (1L << i)) != 0;
  }

  private boolean typeIgnored(int i) {
    return (acceptedTypeBits & (1L << i)) == 0;
  }


  public List<Token> tokenize(File file) throws IOException {
    return getAllTokens(lexerInstance(CharStreams.fromPath(file.toPath())));
  }

  public List<Token> tokenize(String input) {
    return getAllTokens(lexerInstance(CharStreams.fromString(input)));
  }

  public List<String> tokenizeToStrings(String input) {
    List<Token> tokens = tokenize(input);
    List<String> tokenStrings = new ArrayList<>(tokens.size());
    for (Token token : tokens) {
      tokenStrings.add(token.getText());
    }
    return tokenStrings;
  }

  public Iterator<Token> getTokenIterator(String input) {
    return new TokenIterator(this, lexerInstance(CharStreams.fromString(input)));
  }

  public Iterator<Token> getTokenIterator(File file) throws IOException {
    return new TokenIterator(this, lexerInstance(CharStreams.fromPath(file.toPath())));
  }

  private List<Token> getAllTokens(Lexer lexer) {
    List<Token> tokens = new ArrayList<>();
    for (Token token = lexer.nextToken();
        token.getType() != Token.EOF;
        token = lexer.nextToken()) {
      int type = token.getType();
      if (typeIgnored(type)) {
        continue;
      }
      tokens.add(token);
    }
    return tokens;
  }

  public static class Builder {

    private long acceptedTypeBits = ~0L;

    public Builder acceptTypes(int... types) {
      for (int i : types) {
        validateType(i);
        this.acceptedTypeBits |= (1L << i);
      }
      return this;
    }

    public Builder ignoreTypes(int... types) {
      for (int i : types) {
        validateType(i);
        this.acceptedTypeBits &= ~(1L << i);
      }
      return this;
    }

    public Builder ignoreAll() {
      this.acceptedTypeBits = 0L;
      return this;
    }

    public Builder acceptAll() {
      this.acceptedTypeBits = ~0L;
      return this;
    }

    public TurkishTokenizer build() {
      return new TurkishTokenizer(acceptedTypeBits);
    }
  }

  private static class TokenIterator implements Iterator<Token> {

    TurkishLexer lexer;
    TurkishTokenizer tokenizer;
    Token token;

    private TokenIterator(TurkishTokenizer tokenizer, TurkishLexer lexer) {
      this.tokenizer = tokenizer;
      this.lexer = lexer;
    }

    @Override
    public boolean hasNext() {
      Token token = lexer.nextToken();
      if (token.getType() == Token.EOF) {
        return false;
      }
      while (tokenizer.typeIgnored(token.getType())) {
        token = lexer.nextToken();
        if (token.getType() == Token.EOF) {
          return false;
        }
      }
      this.token = token;
      return true;
    }

    @Override
    public Token next() {
      return token;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported");
    }
  }
}
