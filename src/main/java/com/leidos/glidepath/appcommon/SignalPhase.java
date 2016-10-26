package com.leidos.glidepath.appcommon;

public enum SignalPhase {
	GREEN(0),
	YELLOW(1),
	RED(2),
    NONE(3);            // this value is only used for the UI to indicate missing data

	SignalPhase(int val) {
		this.val = val;
	}
	
	public int value() {
		return this.val;
	}
	
	public SignalPhase next() {
		SignalPhase rtn = GREEN;
		
		switch(val) {
		case 0:
			rtn = YELLOW;
			break;
		case 1:
			rtn = RED;
			break;
		case 2:
			rtn = GREEN;
			break;
        default:
            rtn = NONE;
            break;
		}
		return rtn;
	}
	
	private int val;
}
