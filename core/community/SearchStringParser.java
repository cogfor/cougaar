/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.community;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

/**
 * Parse a RFC 2554 search string. A simple recursive descent parser.
 **/
public class SearchStringParser {
    private static final String LP = "(";
    private static final String RP = ")";
    private static final String AND = "&";
    private static final String OR = "|";
    private static final String NOT = "!";
    private static final String SPACE = " ";
    private static final String TAB = "\t";
    private static final String SEPS = LP + RP + AND + OR + NOT;

    private StringTokenizer tokens;
    private String token;
    private String peek;        // Lookahead token

    /**
     * An exception to throw when a parsing error occurs
     **/
    public static class ParseException extends NamingException {
        public ParseException(String msg) {
            super(msg);
        }
    }

    /**
     * Parse a string into a Filter. Creates a StringTokenizer for the
     * parser methods to use and parses the top level "filter"
     * expression.
     * @param s the string to parse
     * @return a Filter that can be used to test Attributes for a
     * match
     * @exception ParseException
     **/
    public synchronized Filter parse(String s) throws ParseException {
        tokens = new StringTokenizer(s, SEPS, true);
        Filter result = filter();
        return result;
    }

    /**
     * Get the next token. Has a one token pushback in the peek
     * variable. Skips whitespace.
     * @return the next token string
     * @exception ParseException if there are no more tokens left
     **/
    private String getToken() throws ParseException {
        if (peek != null) {
            token = peek;
            peek = null;
        } else {
            do {
                if (!tokens.hasMoreTokens()) throw new ParseException("premature end");
                token = tokens.nextToken();
            } while (token != null && token.trim().equals(""));
        }
        return token;
    }

    /**
     * Get the next token but leave it in the token stream
     * @return the next token string
     * @exception ParseException if there are no more tokens left
     **/
    private String peekToken() throws ParseException {
        peek = getToken();
        return peek;
    }

    /**
     * Verify that the next token is as expected.
     * @param expected the token that should come next
     * @exception ParseException if the next token is incorrect
     **/
    private void checkToken(String expected) throws ParseException {
        if (!expected.equals(getToken())) throw new ParseException(expected + " missing");
    }

    /**
     * Parse a "filter", a filtercomp surrounded by parens.
     * @return the resulting Filter
     * @exception ParseException
     **/
    private Filter filter() throws ParseException {
        checkToken(LP);
        Filter result = filtercomp();
        checkToken(RP);
        return result;
    }

    /**
     * Parse a "filtercomp" which is either an AND, OR, or NOT
     * expression or some kind of match expression. All match
     * expressions have no parens, so the appearance of a paren
     * constitutes a syntax error
     * @return the resulting Filter
     * @exception ParseException
     **/
    private Filter filtercomp() throws ParseException {
        getToken();
        if (token.equals(AND)) return new FilterAnd(filterlist());
        if (token.equals(OR)) return new FilterOr(filterlist());
        if (token.equals(NOT)) return new FilterNot(filter());
        if (token.equals(LP)) throw new ParseException(LP + " unexpected");
        if (token.equals(RP)) throw new ParseException(RP + " unexpected");
        return item(token);
    }

    /**
     * Parse a list of one or more "filters". Each "filter" begins
     * with a left paren, so the loop continues as long as that is
     * true.
     * @return an array of Filter objects.
     * @exception ParseException
     **/
    private Filter[] filterlist() throws ParseException {
        List result = new ArrayList();
        do {
            result.add(filter());
        } while (LP.equals(peekToken()));
        return (Filter[]) result.toArray(new Filter[result.size()]);
    }

    /**
     * An item is an attribute description, filtertype, and pattern or
     * value. All filtertypes have an equal sign, so we look for that.
     * Then we check the character preceding the equal sign to see if
     * it is also part of the filter type. The string before the
     * filtertype is the attribute description. The part after is some
     * kind of value or pattern depending on the filtertype. We don't
     * support matching rule items and we don't parse the attribute
     * description any further.
     * @param s String
     **/
    private Filter item(String s) throws ParseException {
        int eqPos = s.indexOf('=');
        if (eqPos < 1) throw new ParseException("filtertype missing");
        String pattern = s.substring(eqPos + 1);
        switch (s.charAt(eqPos - 1)) {
        case '<':
            return new FilterLessThan(s.substring(0, eqPos - 1), pattern);
        case '>':
            return new FilterGreaterThan(s.substring(0, eqPos - 1), pattern);
        case '~':
            return new FilterApproximateMatch(s.substring(0, eqPos - 1), pattern);
        case ':':
            throw new ParseException("matching rules not supported");
        default:
            String attr = s.substring(0, eqPos);
            if (pattern.indexOf('*') < 0) {
                return new FilterEquality(attr, pattern);
            } else if (pattern.length() == 1) {
                return new FilterPresence(attr);
            } else {
                return new FilterSubstring(attr, pattern);
            }
        }
    }

    /**
     * A base class for the following Filter implementations.
     * Implements the usual public toString method in terms of a
     * version of toString accepting a StringBuffer arg. This makes
     * the construction of the string value more efficient.
     **/
    private static abstract class FilterBase {
        public String toString() {
            StringBuffer b = new StringBuffer();
            toString(b);
            return b.toString();
        }
        public abstract void toString(StringBuffer b);
    }

    /**
     * A Filter representing an AND operation.
     **/
    private static class FilterAnd extends FilterBase implements Filter {
        private Filter[] list;

        public FilterAnd(Filter[] list) {
            this.list = list;
        }

        /**
         * Check if all the filters in the filter list match the given
         * Attributes.
         * @return false if any item fails, else return true.
         **/
        public boolean match(Attributes attrs) throws NamingException {
            for (int i = 0; i < list.length; i++) {
                if (!list[i].match(attrs)) return false;
            }
            return true;
        }

        /**
         * Append our contribution to the overall string.
         **/
        public void toString(StringBuffer b) {
            b.append("(&");
            for (int i = 0; i < list.length; i++) {
                list[i].toString(b);
            }
            b.append(")");
        }
    }

    /**
     * A Filter representing an OR operation.
     **/
    private static class FilterOr extends FilterBase implements Filter {
        private Filter[] list;

        public FilterOr(Filter[] list) {
            this.list = list;
        }

        /**
         * Check if any of the filters in the filter list match the given
         * Attributes.
         * @return true if any item matches, else return false.
         **/
        public boolean match(Attributes attrs) throws NamingException {
            for (int i = 0; i < list.length; i++) {
                if (list[i].match(attrs)) return true;
            }
            return false;
        }

        /**
         * Append our contribution to the overall string.
         **/
        public void toString(StringBuffer b) {
            b.append("(|");
            for (int i = 0; i < list.length; i++) {
                list[i].toString(b);
            }
            b.append(")");
        }
    }

    /**
     * A filter that negates the value of another Filter
     **/
    private static class FilterNot extends FilterBase implements Filter {
        private Filter filter;

        public FilterNot(Filter filter) {
            this.filter = filter;
        }

        /**
         * Return the negation of applying filter.
         **/
        public boolean match(Attributes attrs) throws NamingException {
            return !filter.match(attrs);
        }

        /**
         * Append our contribution to the overall string.
         **/
        public void toString(StringBuffer b) {
            b.append("(!");
            filter.toString(b);
            b.append(")");
        }
    }

    /**
     * A Filter that checks if the value of an attribute is less than
     * a specified value. This version simply compares strings.
     **/
    private static class FilterLessThan extends FilterBase implements Filter {
        private String attrdesc, value;

        public FilterLessThan(String attrdesc, String value) {
            this.attrdesc = attrdesc;
            this.value = value;
        }

        public boolean match(Attributes attrs) throws NamingException {
            try {
                Attribute attr = attrs.get(attrdesc);
                for (int i = 0, n = attr.size(); i < n; i++) {
                  String attrValue = attr.get(i).toString();
                  if (attrValue.compareTo((String)attrs.get(attrdesc).get(i)) <=0) {
                    return true;
                  }
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Append our contribution to the overall string.
         **/
        public void toString(StringBuffer b) {
            b.append("(");
            b.append(attrdesc);
            b.append("<=");
            b.append(value);
            b.append(")");
        }
    }

    /**
     * A Filter that checks if the value of an attribute is greater than
     * a specified value. This version simply compares strings.
     **/
    private static class FilterGreaterThan extends FilterBase implements Filter {
        private String attrdesc, value;

        public FilterGreaterThan(String attrdesc, String value) {
            this.attrdesc = attrdesc;
            this.value = value;
        }

        public boolean match(Attributes attrs) throws NamingException {
            try {
                Attribute attr = attrs.get(attrdesc);
                for (int i = 0, n = attr.size(); i < n; i++) {
                  String attrValue = attr.get(i).toString();
                  if (attrValue.compareTo((String)attrs.get(attrdesc).get(i)) >=0) {
                    return true;
                  }
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Append our contribution to the overall string.
         **/
        public void toString(StringBuffer b) {
            b.append("(");
            b.append(attrdesc);
            b.append(">=");
            b.append(value);
            b.append(")");
        }
    }

    /**
     * A Filter that checks if an attribute is present
     **/
    private static class FilterPresence extends FilterBase implements Filter {
        private String attrdesc;

        public FilterPresence(String attrdesc) {
            this.attrdesc = attrdesc;
        }

        public boolean match(Attributes attrs) throws NamingException {
            try {
                return attrs.get(attrdesc) != null;
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Append our contribution to the overall string.
         **/
        public void toString(StringBuffer b) {
            b.append("(");
            b.append(attrdesc);
            b.append("=*)");
        }
    }

    /**
     * A Filter that checks if the value of an attribute is equal to
     * a specified value. This version simply compares strings.
     **/
    private static class FilterEquality extends FilterBase implements Filter {
        protected String attrdesc, value;

        public FilterEquality(String attrdesc, String value) {
            this.attrdesc = attrdesc;
            this.value = value;
        }

        public boolean match(Attributes attrs) throws NamingException {
            try {
                Attribute attr = attrs.get(attrdesc);
                for (int i = 0, n = attr.size(); i < n; i++) {
                    String attrValue = attr.get(i).toString();
                    if (value.equals(attrValue)) return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Append our contribution to the overall string.
         **/
        public void toString(StringBuffer b) {
            b.append("(");
            b.append(attrdesc);
            b.append("=");
            b.append(value);
            b.append(")");
        }
    }

    /**
     * A Filter that checks if the value of an attribute is
     * approximately equal to a specified value. This version is the
     * same as FilterEquality.
     **/
    private static class FilterApproximateMatch extends FilterEquality {
        public FilterApproximateMatch(String attrdesc, String value) {
            super(attrdesc, value);
        }

        /**
         * Append our contribution to the overall string.
         **/
        public void toString(StringBuffer b) {
            b.append("(");
            b.append(attrdesc);
            b.append("~=");
            b.append(value);
            b.append(")");
        }
    }

    /**
     * A Filter that checks if the value of an attribute matches
     * a specified pattern.
     **/
    private static class FilterSubstring extends FilterBase implements Filter {
        private String attrdesc;
        private Glob glob;

        public FilterSubstring(String attrdesc, String value) {
            this.attrdesc = attrdesc;
            this.glob = Glob.parse(value);
        }

        public boolean match(Attributes attrs) throws NamingException {
            try {
              Attribute attr = attrs.get(attrdesc);
              for (int i = 0; i < attr.size(); i++) {
                if (glob.match(attrs.get(attrdesc).get(i).toString())) {
                  return true;
                }
              }
              return false;
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Append our contribution to the overall string.
         **/
        public void toString(StringBuffer b) {
            b.append("(");
            b.append(attrdesc);
            b.append("=");
            glob.appendString(b);
            b.append(")");
        }
    }
}
