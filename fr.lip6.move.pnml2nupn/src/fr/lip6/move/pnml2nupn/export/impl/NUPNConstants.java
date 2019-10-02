package fr.lip6.move.pnml2nupn.export.impl;

public final class NUPNConstants {
	
	public static final String NUPN_SUPPORTED_VERSION = "1.1";
	public static final String TEXT = "text";
	public static final String INSCRIPTION = "inscription";
	public static final String TRANS_EXT = ".trans";
	public static final String STATES_EXT = ".places";
	public static final String UNSAFE_ARC = ".unsafe.arcs";
	public static final String STOP = "STOP";
	public static final String CANCEL = "CANCEL";
	public static final String NL = "\n";
	public static final String HK = "#";
	public static final String PLACES = "places";
	public static final String UNITS = "units";
	public static final String U = "U";
	public static final String INIT_PLACE = "initial place";
	public static final String INIT_PLACES = "initial places";
	public static final String ROOT_UNIT = "root unit";
	public static final String TRANSITIONS = "transitions";
	public static final String T = "T";
	public static final String WS = " ";
	public static final String ZERO = "0";
	public static final String ONE = "1";
	public static final String DOTS = "...";
	public static final String COMMA = ",";
	public static final String COMMAWS = COMMA + WS;
	public static final String STRUCTURE = "structure";
	public static final String P_PREFX = "p";
	public static final String T_PREFX = "t";
	public static final String LABELS_1_0_0 = "labels 1 0 0"; // When there is no transition
	public static final String LABELS_1_1_0 = "labels 1 1 0"; // When there is at least one transition
	public static final String NO_NAME_PREFIX = "_";          // prefix for empty place or transition names

	private NUPNConstants() {super();}
	
}
