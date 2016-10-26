package com.leidos.glidepath.appcommon;

public class IntDataElement extends DataElement {

	public IntDataElement(int val){
		super();
		value_ = val;
	}
	
	public int				value(){
		//returns the value of the data element
		
		return value_;
	}
	
	////////////////////////////////////////////////
	
	protected int value_; //element value
}
