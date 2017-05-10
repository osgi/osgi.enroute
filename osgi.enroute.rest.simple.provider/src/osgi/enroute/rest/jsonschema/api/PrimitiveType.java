package osgi.enroute.rest.jsonschema.api;

public enum PrimitiveType {
	STRING("string"), INTEGER, NUMBER, BOOLEAN, ARRAY, OBJECT, NONE;

	private final String name;

	private PrimitiveType(String name) {
		this.name = name;
	}

	private PrimitiveType() {
		this.name = super.toString().toLowerCase();
	}
	
	public String toString() {
		return name;
	}
}
