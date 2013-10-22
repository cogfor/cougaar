/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.adaptivity;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.service.LoggingService;

/**
 * Parser for plays to be stored in a Playbook. The grammar is
 * straightforward with the exception of ranges and range lists.
 * <h3>Basic Syntax</h3>
 * <p>Each play consists of an if clause (predicate) and one or more
 * constraint phrases. These are all separated by colons and
 * terminated with a semi-colon. For example:</p>
 * <pre>&lt;if clause&gt;:&lt;constraint&gt;:...&lt;constraint&gt;;</pre><p>
 * The &lt;if clause&gt; is an expression involving {@link Condition}s and
 * constants that must evaluate to true or false. A constraint
 * consists of an {@link OperatingMode} following by a
 * ConstraintOperator and a range list and signifies that the
 * OperatingMode will be constrained so as to satisfy the relation
 * specified by the operator to the range list. For example:</p>
 * <pre>
 * FooPlugin.MODE &lt; 5;
 * FooPlugin.MODE in {1 to 7};
 * FooPlugin.SPEED = "FAST";</pre><p>
 * Note that both relational and range operators (==, not it, etc.) as
 * well as assignment may be used. Assignment explicitly specifies the
 * allowed values. Relational and range operators implicitly specify
 * the allowed values by specifying the test that any such value must
 * satisfy. The operators =, ==, and in are all equivalent as are !=
 * and not in. The other relational operators are usually used with
 * single valued range lists (constants).
 * <h3>Comments</h3><p>
 * Comments are ignored by the parser. Both slash-slash and slash-star
 * comments are recognized. The former beging with two adjacent
 * slashes and terminate at the end of line. The latter terminate with
 * a star-slash sequence.</p>
 * <h3>Numeric Constants</h3><p>
 * Numeric constants are interpreted as floating point numbers
 * (doubles). When necessary, numbers will be automatically cast to integers or
 * longs. A consequence of this is that plays may not use the full
 * precision offered by longs (64 bits) and are restricted to the 56
 * bits of precision in a double.
 * <h3>Ranges</h3><p>
 * A range is written as two numbers separated by the keyword "to" or
 * "thru". The former excludes the end point and the latter includes
 * the end point. A point range (a range having exactly one value) may
 * be written as a single number; 7 is equivalent to 7 thru 7. 7 to 7,
 * on the other hand, is an empty range; it allows no values.
 * <h3>Range Lists</h3><p>
 * A range list is a sequence of ranges enclosed in braces. The
 * elements of a range list may be separated by commas, but they are
 * not required; white space is sufficient. A range
 * list consisting of exactly one range my omit the braces.
 * Consequently, a numeric constant is also a range list. Range lists
 * frequently have elements that are point ranges and express list of
 * descrete values rather than a continuum. Range lists
 * may appear only as the right-hand operand of relational operators
 * (see below). Numeric constants may appear in arithmetic expressions.
 * to the written value. In general the context makes it clear which
 * should be used, but for example:</p><pre>
 * Foo &lt; {11 to 20, 25 thru 30, 1 to 3};</pre><p>
 * is true if Foo is less than 1 (the minimum of the ranges). This
 * characteristic is an artifact of the parser and probably
 * insignificant to the playbook writer.</p><p>
 * As implied above, a range list consists of one or more ranges
 * enclosed in braces. The comma between the ranges is optional (white
 * space is sufficient). The braces are also optional if the list has
 * a single range. Each range consists of either one number or string
 * or two numbers or strings separated by either "to" or "thru". "To"
 * signifies a range that does not include the end point whereas
 * "thru" signifies a range that does include the end point. If the
 * number is floating point, only the exact value given by the end
 * point is excluded. The next smaller value that can be represented
 * is always included. This characteristic can be used to insure there
 * are no gaps in the coverage of a series of predicates. For
 * example:</p><pre>
 * x in {1 to 3, 5 to 10}:...;
 * x in {3 to 5}:...;
 * x &gt;= 10:...;</pre><p>insures that exactly one of the predicates
 * is true for any
 * value of x from 1 to infinity. There is no value of x that can fall
 * into a crack in the vicinity of 3 or 5 or 10 nor is there any value of x
 * in those same regions that can cause two predicates to
 * fire.</p><p>Numbers are parsed as doubles and coerced to other
 * numeric typs as needed. This means that long values cannot be
 * written with their entire range. (Doubles can exactly represent
 * only 56 bits of precision.)</p>
 * <h3>Strings</h3><p>Strings are used as constants (range limits),
 * Condition names and OperatingMode names. The interpretation depends
 * on context (see below). Strings do not need to be quoted unless
 * they contain characters (such as spaces) that have syntactic
 * meaning to the parser. In particular, strings need not be quoted
 * when they contain . (period) and [] (brackets). All other
 * punctuation and special characters should be quoted.</p><p>
 * String constants can be used in range lists and arithmetic
 * expressions using the + operator. Strings cannot be used in other
 * arithmetic expressions. String comparisons are usually
 * confined to equality and inequality tests, but the other operators
 * have a defined meaning (alphabetic comparison using the default
 * collation sequence). String ranges are rare, but if used, have a
 * slightly different meaning when "to" ranges are specified because
 * the highest value included in a "to" range would be infinitely
 * long. For example, the last string in the range "bar" to "foo"
 * would be "fon\uffff\uffff\uffff\uffff...". It is hard to conceive of
 * a use for a "to" range involving strings, so the infinite string is
 * truncated after the first "\uffff".</p>
 * Strings that name Conditions <em>can</em> be used in arithmetic
 * expressions if the named Condition has a numeric value. If the
 * named Condition has a String value, then only the + operator is
 * allowed.
 * <h3>If Clause Operators</h3>
 * <h4>Arithmetic Operators (+, -, *, /)</h4><p>
 * These have their standard meanings. The parser treats these with
 * standard precedence so parentheses are needed in the usual places.
 * When in doubt, parenthesize. There is no modulus operator (%).</p>
 * <h4>Relational Operators (&lt; &lt;= == != &gt;= &gt;)</h4><p>
 * Relational operators compare two quantities and yield a boolean
 * (true or false) result. Relational operators are <em>not</em>
 * commutative; the interpretation of the left hand operand is
 * different from the right hand operand. Strings in the left hand
 * operand always name Conditions in if clauses or OperatingModes in
 * constraints. Strings in the right hand operand are always values
 * (or range limits). So, for example, in the play:</p><pre>
 * FOO == HIGH: HIGH = FOO;</pre><p>The first FOO is the name of a
 * Condition, the second FOO is a value to be stored in the
 * OperatingMode named HIGH when the Condition named FOO has a value
 * equal to HIGH. For clarity, it is a good practice to quote strings
 * used as values and not quote strings that denote Conditions and
 * Operating Modes.
</p><p>Comparison of strings uses the default
 * collation sequence for characters.</p><h4>
 * Range Operators (in and not in)</h4><p>
 * Range operators test for inclusion in (or exclusion from) a range
 * list. The meaning is straightforward:</p><pre>
 * x in {1 thru 7, 10}</pre><p>is true iff
 * x has a value between 1 and 7 inclusive or has the value 10. A
 * range list is just a shorthand for a combination of less than and
 * greater than terms, but is easier to write and process.</p>
 * <h4>Boolean Constants</h4><p>The boolean constants true and false
 * may be used in an if clause. This is useful when developing a
 * playbook before the details are worked out or to deactivate certain
 * plays.</p>
 * <h3>Constraints</h3><p>
 * Constraints specify an allowed list of ranges to which an
 * <code>OperatingMode</code> can be set. The constraints from all the
 * plays (including those manufactured from policies) are combined
 * (intersected) to form the final constraint. Often this final
 * constraint is a single value, but when it has multiple values, the
 * minimum of the first range in the list is used. The constraint can
 * be viewed either as an expression that must be true or as an
 * assignment of a range list to the <code>OperatingMode</code>. For
 * example:</p><pre>
 * Opmode1 = 3
 * Opmode1 in {3 thru 3}
 * Opmode1 == 3</pre><p>
 * are all equivalent. The first assigns the single valued range {3
 * thru 3} to Opmode1. The second requires that Opmode1 have the value
 * 3 so that is in the range 3 thru 3. The third requires that Opmode1
 * be exactly equal to 3.</p><p>All forms of constraints are rewritten
 * as an assignment of a range list to the <code>OperatingMode</code>.
 * For example:</p><pre>
 * Opmode < 3</pre><p>
 * is rewritten as:</p><pre>
 * Opmode = {&lt;negative infinity&gt; to 3}</pre><p>
 * The process of intersecting the constraints from several plays may
 * not be clear. For example, if the if clauses of two places yielded
 * these two constraints:</p><pre>
 * opmode in {56, 128, 256, 1024}
 * opmode >= 128</pre><p>The combined constraint would be:</p><pre>
 * opmode in {128, 256, 1024}</pre><p>Another example:</p><pre>
 * opmode in 1 thru 10
 * opmode in 5 to 20</pre><p>The combined result would be:
 * opmode in 5 thru 10</pre><p>For a value to be included in the
 * combined range list, it must be included by the range lists of all
 * the selected plays. It is a playbook-writing error to allow plays
 * to be simultaneously active for which the combined range list is
 * empty. For example:</p><pre>
 * opmode in {56, 128, 256}
 * opmode < 56</pre><p>would be an error. Such errors are not detected
 * until they occur. When they do occur, they are logged and the later
 * constraint is ignored.</p><h3>OperatingModeConditions</h3><p>
 * OperatingModeConditions are intermediate variables the act as
 * OperatingModes in some plays and Conditions in later plays. This
 * allows certain plays such as those that result from policies
 * received from other agents to set the value of an agent-wide
 * OperatingMode that is then translated into component-specific
 * OperatingModes by using the agent-wide mode as a Condition in other
 * plays. For example, assume the following plays resulting from
 * inter-agent operating mode policies:</p><pre>
 * Threatcon > 3: DefensePosture = 2;
 * Threatcon <= 3: DefensePosture = 1;</pre><p>
 * and the following plays from the local playbook:</p><pre>
 * DefensePosture < 2: FooPlugin.keyLength = 56: AdaptivePlugin.fidelity = high;
 * DefensePosture >= 2: FooPlugin.keylength = 128: AdaptivePlugin.fidelity = medium;</pre><p>
 * The allows the outside agent to be unaware of the details of the
 * makeup of the agent by expressing its policy in terms of a
 * "DefensePosture" concept. However, the local playbook is developed with an
 * awareness of the agent makeup so it can contain plays to set the
 * operating modes of its plugins based on this
 * DefensePosture value.</p><h3>OperatingModePolicy</h3><p>
 * An OperatingModePolicy has the same syntax as a Play except that it
 * is prefixed with a name.</p>
 **/
public class Parser {
  StreamTokenizer st;
  boolean pushedBack = false;
  ConstrainingClause cc;
  private LoggingService logger;

  /**
   * Construct from a Reader.
   * @param s the Reader to read from
   * @param logger a LoggingService onto which error and debug
   * information will be written.
   **/
  public Parser(Reader s, LoggingService logger) {
    this.logger = logger;
    st = new StreamTokenizer(s);
    st.wordChars('_', '_');
    st.wordChars('[', '[');
    st.wordChars(']', ']');
    st.ordinaryChars('/', '/');
    st.slashStarComments(true);
    st.slashSlashComments(true);
    st.quoteChar('"');
    st.quoteChar('\'');
  }

  /**
   * Parses and returns the next ConstrainingClause. The terminator
   * (colon) is not consumed.
   * @return the parsed constraining clause.
   * @throws IOException if an error occurs reading from the input.
   **/
  public ConstrainingClause parseConstrainingClause() throws IOException {
    cc = new ConstrainingClause();
    parse(Operator.MAXP);
    ConstrainingClause result = cc;
    cc = null;
    return result;
  }

  /**
   * Parse a series of constraints from the Reader up through a
   * semicolon. The terminating semicolon is consumed.
   * @return an array of ConstraintPhrases parsed from the input.
   * @throws IOException if an error occurs reading from the input.
   **/
  public ConstraintPhrase[] parseConstraints() throws IOException {
    List constraintPhrases = new ArrayList();
    while (true) {
      int token = nextToken();
      if (token == ';') {
        break;
      }
      if (token != ':') throw unexpectedTokenException("Missing semicolon");
      constraintPhrases.add(parseConstraintPhrase());
    }

    return (ConstraintPhrase[]) constraintPhrases.toArray(new ConstraintPhrase[constraintPhrases.size()]);
  }

  /**
   * Parses a single, complete Play from the input. The terminating
   * semicolon is consumed.
   * @return the parsed Play.
   * @throws IOException if an error occurs reading from the input.
   **/
  public Play parsePlay() throws IOException {
    ConstrainingClause cc = parseConstrainingClause();          // Parse the ifClause
    ConstraintPhrase[] cp = parseConstraints();
    return new Play(cc, cp);
  }

  /**
   * Parses an entire file of Plays and returns them as an array.
   * @return an array of plays from the file.
   * @throws IOException if an error occurs reading from the input.
   **/
  public Play[] parsePlays() throws IOException {
    List plays = new ArrayList();
    readPlays:
    while (true) {
      try {
        plays.add(parsePlay());
      } catch (IllegalArgumentException e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Parse exception", e);
        } else {
          logger.error(e.getMessage());
        }
        while (st.ttype != ';') {
          if (st.ttype == StreamTokenizer.TT_EOF) break readPlays;
        }
      }
      if (nextToken() == StreamTokenizer.TT_EOF) break;
      pushBack();
    }
    return (Play[]) plays.toArray(new Play[plays.size()]);
  }

  /**
   * Parses an OperatingModePolicy. Syntactically, an
   * OperatingModePolicy is identical with a play except it is
   * prefixed with a name token. The name token must be a string
   * (quoted as necessary).
   * @return an OperatingModePolicy
   * @throws IOException if an error occurs reading from the input.
   **/
  public OperatingModePolicy parseOperatingModePolicy() throws IOException {
    // pull the first token, assume it is the policy name
    String policyName = null;
    if (isWord(nextToken())) {
      policyName = st.sval;
    } else {
      pushBack();
    }
    ConstrainingClause cc = parseConstrainingClause();          // Parse the ifClause
    ConstraintPhrase[] cp = parseConstraints();
    return new OperatingModePolicy(policyName, cc, cp);
  }

  /**
   * Parses an entire file of OperatingModePolicies and returns them
   * as an array.
   * @return an array of OperatingModePolicies from the file.
   * @throws IOException if an error occurs reading from the input.
   **/
  public OperatingModePolicy[] parseOperatingModePolicies() throws IOException {
    List policies = new ArrayList();
    while (true) {
      if (nextToken() == StreamTokenizer.TT_EOF) break;
      pushBack();
      policies.add(parseOperatingModePolicy());
    }
    return (OperatingModePolicy[]) policies.toArray(new OperatingModePolicy[policies.size()]);
  }

  /**
   * Parse an if clause and push it onto the ConstrainingClause.
   * @param lp left left precedence of operator calling this method.
   */
  private void parse(int lp) throws IOException {
    if (logger.isDebugEnabled()) {
      String caller = new Throwable().getStackTrace()[1].toString();
      logger.debug("Parse from " + caller + " " + lp);
    }
    try {
      int token = nextToken();
      switch (token) {
      default:
        throw unexpectedTokenException("Unexpected token");
      case '!':
        parse(BooleanOperator.NOT.getLP());
        cc.push(BooleanOperator.NOT);
        break;
      case '-':
        parse(ArithmeticOperator.NEGATE.getLP());
        cc.push(ArithmeticOperator.NEGATE);
        break;
      case '(':
        parse(Operator.MAXP);
        token = nextToken();
        if (token != ')') throw unexpectedTokenException("Missing close paren");
        break;
      case '"':
      case '\'':
      case StreamTokenizer.TT_WORD:
        if (st.sval.equalsIgnoreCase(BooleanOperator.TRUE.toString())) {
          cc.push(BooleanOperator.TRUE);
          break;
        }
        if (st.sval.equalsIgnoreCase(BooleanOperator.FALSE.toString())) {
          cc.push(BooleanOperator.FALSE);
          break;
        }
        cc.push(st.sval);
        break;
      case StreamTokenizer.TT_NUMBER:
        cc.push(new Double(st.nval));
        break;
      }
      while (true) {
        token = nextToken();        // Operator
        if (token == ':') {
          pushBack();
          return;
        }
        Operator op;
        switch (token) {
        default: pushBack(); return;
        case '"':
        case '\'':
        case StreamTokenizer.TT_WORD:
          if (!st.sval.equalsIgnoreCase("in") && !st.sval.equalsIgnoreCase("not")) {
            pushBack();
            return;
          }
          // Fall thru into parseConstraintOpValue
        case '<':
        case '>':
        case '=':
        case '!':
          if (ConstraintOperator.IN.getRP() >= lp) {
            // Not yet
            pushBack();
            return;
          }
          pushBack();
          cc.push(parseConstraintOpValue(null));
          return;
        case '&': op = BooleanOperator.AND; break;
        case '|': op = BooleanOperator.OR; return;
        case '+': op = ArithmeticOperator.ADD; break;
        case '-': op = ArithmeticOperator.SUBTRACT; break;
        case '*': op = ArithmeticOperator.MULTIPLY; break;
        case '/': op = ArithmeticOperator.DIVIDE; break;
        }
        int opp = op.getRP();
        if (opp < lp) {
          parse(opp);
          cc.push(op);
          continue;
        } else {
          pushBack();
          return;
        }
      }
    } finally {
      if (logger.isDebugEnabled()) logger.debug("Exit parse " + lp);
    }
  }

  private ConstraintPhrase parseConstraintPhrase() throws IOException {
    if (!isWord(nextToken())) throw unexpectedTokenException("Expected OperatingMode name");
    ConstraintPhrase cp = new ConstraintPhrase(st.sval);
    parseConstraintOpValue(cp);
    return cp;
  }

  private ConstraintOpValue parseConstraintOpValue(ConstraintOpValue cov) throws IOException {
    int token1 = nextToken();
    if (cov == null) cov = new ConstraintOpValue();
    
    switch (token1) {
    case '<':
    case '>':
    case '=':
    case '!':
      int token2 = nextToken();
      if (token2 == '=') {
        switch (token1) {
        case '<':
          cov.setOperator(ConstraintOperator.LESSTHANOREQUAL);
          break;
        case '>':
          cov.setOperator(ConstraintOperator.GREATERTHANOREQUAL);
          break;
        case '=':
          cov.setOperator(ConstraintOperator.EQUAL);
          break;
        case '!':
          cov.setOperator(ConstraintOperator.NOTEQUAL);
          break;
        }
        token1 = nextToken();
      } else {
        switch (token1) {
        case '=':
          cov.setOperator(ConstraintOperator.ASSIGN);
          break;
        case '<':
          cov.setOperator(ConstraintOperator.LESSTHAN);
          break;
        case '>':
          cov.setOperator(ConstraintOperator.GREATERTHAN);
          break;
        default:
          throw unexpectedTokenException("Malformed ConstraintOperator");
        }
        token1 = token2;
      }
      break; // end of punctuation chars case
    case '"':
    case '\'':
    case StreamTokenizer.TT_WORD:
      if (st.sval.equalsIgnoreCase(ConstraintOperator.IN.toString())) {
        cov.setOperator(ConstraintOperator.IN);
        token1 = nextToken();
        break;
      }
      if (st.sval.equalsIgnoreCase("NOT")) {
        if (isWord(nextToken()) && st.sval.equalsIgnoreCase("IN")) {
          cov.setOperator(ConstraintOperator.NOTIN);
          token1 = nextToken();
          break;
        }
      }
    default: 
      throw unexpectedTokenException("Missing ConstraintOperator");
    }
    if (isWord(token1)) {
      cov.setAllowedValues(new OMCRangeList(parseRange(st.sval)));
    } else if (token1 == StreamTokenizer.TT_NUMBER) {
      cov.setAllowedValues(new OMCRangeList(parseRange(new Double(st.nval))));
    } else if (token1 == '{') {
      cov.setAllowedValues(parseSet());
    } else {
      throw unexpectedTokenException("Expected range list");
    }
    return cov;
  }

  private OMCRange parseRange(Comparable first) throws IOException {
    Class elementClass = first.getClass();
    int token = nextToken();
    if (isWord(token)) {
      boolean isTo = st.sval.equalsIgnoreCase("to");
      boolean isThru = st.sval.equalsIgnoreCase("thru");
      if (isTo || isThru) {
        Comparable last;
        token = nextToken();
        if (isWord(token)) {
          if (elementClass == Double.class) {
            throw unexpectedTokenException("Number expected");
          }
          last = st.sval;
        } else if (token == StreamTokenizer.TT_NUMBER) {
          if (elementClass == String.class) {
            throw unexpectedTokenException("String expected");
          }
          last = new Double(st.nval);
        } else {
          throw unexpectedTokenException("Expected " + (elementClass == Double.class ? "number" : "string"));
        }
        if (isTo) {
          return new OMCToRange(first, last);
        } else {
          return new OMCThruRange(first, last);
        }
      }
    }
    pushBack();
    return new OMCPoint(first);
  }

  private OMCRangeList parseSet() throws IOException {
    Class elementClass = null;
    List values = new ArrayList();

    while (true) {
      int token1 = nextToken();
      if (isWord(token1)) {
        if (elementClass == Double.class) {
          throw unexpectedTokenException("Number expected");
        } else if (elementClass == null) {
          elementClass = String.class;
        }
        values.add(parseRange(st.sval));
      } else if (token1 == StreamTokenizer.TT_NUMBER) {
        if (elementClass == String.class) {
          throw unexpectedTokenException("String expected");
        } else if (elementClass == null) {
          elementClass = Double.class;
        }
        values.add(parseRange(new Double(st.nval)));
      } else if (token1 == ',') {
        // ignore comma
      } else if (token1 == '}') {
        break;
      } else {
        throw unexpectedTokenException("Missing close brace");
      }
    }
    return new OMCRangeList((OMCRange[]) values.toArray(new OMCRange[values.size()]));
  }

  private void pushBack() {
    pushedBack = true;
  }

  private boolean isWord(int token) {
    return token == StreamTokenizer.TT_WORD || token == '"' || token == '\'';
  }

  private int nextToken() throws IOException {
    int token;
    String caller = null;
    if (logger.isDebugEnabled()) caller = new Throwable().getStackTrace()[1].toString();
    if (pushedBack) {
      pushedBack = false;
      token = st.ttype;
      if (logger.isDebugEnabled()) logger.debug("nextToken from " + caller + ": repeat " + tokenAsString());
    } else {
      token = st.nextToken();
      if (logger.isDebugEnabled()) logger.debug("nextToken from " + caller + ": token " + tokenAsString());
    }
    return token;
  }

  private String tokenAsString() {
    switch (st.ttype) {
    case '"':
    case '\'':
    case StreamTokenizer.TT_WORD:
      return st.sval;
    case StreamTokenizer.TT_NUMBER:
      return String.valueOf(st.nval);
    case StreamTokenizer.TT_EOF:
      return "<eof>";
    default:
      return String.valueOf((char) st.ttype);
    }
  }

  private IllegalArgumentException unexpectedTokenException(String mm) {
    String line = "line " + st.lineno();
    String mesg = tokenAsString();
    IllegalArgumentException result =
      new IllegalArgumentException(mm + " on " + line + " found " + mesg);
    StackTraceElement[] trace = result.getStackTrace();
    StackTraceElement[] callerTrace = new StackTraceElement[trace.length - 1];
    System.arraycopy(trace, 1, callerTrace, 0, callerTrace.length);
    result.setStackTrace(callerTrace);
    return result;
  }
}
