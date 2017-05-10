package osgi.enroute.rest.openapi.api;

public @interface ItemType {
	CollectionFormat collectionFormat() default CollectionFormat.none;
}
