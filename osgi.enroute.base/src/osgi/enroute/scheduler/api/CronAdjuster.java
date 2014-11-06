package osgi.enroute.scheduler.api;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// http://www.cronmaker.com/
// http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger

public class CronAdjuster implements TemporalAdjuster {
	List<TemporalAdjuster>	adjusters	= new ArrayList<>();

	public CronAdjuster(String specification) {
		String parts[] = specification.trim().split("\\s+");
		if (parts.length < 6 || parts.length > 7)
			throw new IllegalArgumentException("Invalid cron expression, too many fields. Only 6 or 7 allowed: "
					+ specification);

		seconds(parts[0]);
		minutes(parts[1]);
		hours(parts[2]);
		dayOfMonth(parts[3]);
		month(parts[4]);
		dayOfWeek(parts[5]);
		if (parts.length > 6)
			year(parts[6]);
	}

	static class Expr {
		boolean			L;
		boolean			W;
		List<Integer>	numbers	= new ArrayList<>();
		public int	floor;
		public int	ceiling;
	}

	private void seconds(String string) {
		if (string.equals("*"))
			return;

		Expr expr = toRange(string, 0, 59, null);
		adjusters.add((temporal) -> {
			
			return null;
		});
	}

	// http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger
	static Pattern	TOKENS_P	= Pattern
										.compile(
												"([*-/LW,#]|\\d+|(SUN|MON|TUE|WED|THU|FRI|SAT|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))",
												Pattern.CASE_INSENSITIVE);

	enum Type {
		WILDCARD, DONCARE, RANGE, INCREMENT, LAST, WEEKDAY, COMMA, DAYNR, NUMBER;
	}

	static class Token {
		Type	type;
		int		value;

		public Token(Type type, int number) {
			this.type = type;
			value = number;
		}
	}

	List<Token> tokens(String input, String values[]) {
		List<Token> tokens = new ArrayList<Token>();
		Matcher m = TOKENS_P.matcher(input);
		if (m.lookingAt()) {
			switch (m.group(1)) {
				case "*" :
					tokens.add(new Token(Type.WILDCARD, 0));
					break;
				case "/" :
					tokens.add(new Token(Type.INCREMENT, 0));
				case "-" :
					tokens.add(new Token(Type.RANGE, 0));
					break;
				case "," :
					tokens.add(new Token(Type.COMMA, 0));
					break;
				case "#" :
					tokens.add(new Token(Type.DAYNR, 0));
					break;
				default :
					if (m.group(2) != null)
						tokens.add(new Token(Type.NUMBER, Integer.parseInt(m.group(1))));
					else {
						for (int n = 0; n < values.length; n++) {
							if (values[n].equals(m.group(2))) {
								tokens.add(new Token(Type.NUMBER, n));
								break;
							}
						}
						throw new IllegalArgumentException("Invalid symbolic value " + m.group(2));
					}
					break;
			}
		}
		return tokens;
	}

	private Expr toRange(String string, int floor, int ceiling, String[] values) {
		if (string.equals("*") || string.equals("?"))
			return null;

		Expr expr = new Expr();
		expr.floor = floor;
		expr.ceiling = ceiling;
		List<Token> tokens = tokens(string, values);
		for ( int i=tokens.size(); i>=0; i--) {
			Token token = tokens.get(i);
			switch(token.type) {
				
			}
		}
		parseExpr(expr, tokens);
		return expr;

	}

	private void parseExpr(Expr expr, List<Token> tokens) {
		int value;

		Token first = tokens.remove(0);

		switch (first.type) {
			case WILDCARD :
				value = 0;
				break;

			case NUMBER :
				value = first.value;
				break;

			// If used in the day-of-week field by itself, it simply means "7"
			// or "SAT".
			case LAST :
				value = expr.ceiling;
				break;

			default :
				throw new IllegalArgumentException("Unexpected token " + first.type);
		}

		if (tokens.isEmpty() || tokens.get(0).type == Type.COMMA) {
			expr.numbers.add(value);
			if (!tokens.isEmpty())
				parseExpr(expr, tokens);
			return;
		}

		Token operator = tokens.remove(0);

		switch (operator.type) {

			case INCREMENT :
				increment(expr, tokens, value);
				break;

			case RANGE :
				range(expr,tokens, value);
				break;

			case LAST :
				expr.L = true;
				break;

			case WEEKDAY :
				expr.W = true;
				break;

			default :
				throw new IllegalArgumentException("Unexpected token " + operator.type);
		}

	}

	private void increment(Expr expr, List<Token> tokens, int start) {
		Token token = tokens.remove(0);
		switch(token.type) {
			case NUMBER:
				int v = start;
				while ( v <= expr.ceiling ) {
					expr.numbers.add(v);
					v+= token.value;
				}
				break;
				
			default :
				throw new IllegalArgumentException("Unexpected token " + token.type);
		}
	}

	private void range(Expr expr, List<Token> tokens, int start) {
		Token token = tokens.remove(0);
		switch(token.type) {
			case NUMBER:
				for ( int i=start; i<=expr.ceiling && i<=token.value; i++) {
					expr.numbers.add( i );
				}
				break;
				
			default :
				throw new IllegalArgumentException("Unexpected token " + token.type);
		}
	}

	private void year(String string) {
		if (string.equals("*"))
			return;

	}

	private void dayOfWeek(String string) {
		if (string.equals("*") || string.equals("?"))
			return;

	}

	private void month(String string) {
		if (string.equals("*"))
			return;

	}

	private void dayOfMonth(String string) {
		if (string.equals("*"))
			return;

	}

	private void hours(String string) {
		if (string.equals("*"))
			return;

	}

	private void minutes(String string) {
		if (string.equals("*"))
			return;
	}

	@Override
	public Temporal adjustInto(Temporal temporal) {
		// TODO Auto-generated method stub
		return null;
	}

}
