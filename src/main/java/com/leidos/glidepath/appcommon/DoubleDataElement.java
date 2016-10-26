package com.leidos.glidepath.appcommon;

public class DoubleDataElement extends DataElement {

	public DoubleDataElement(double val){
		super();
		value_ = val;
	}
	
	public double				value(){
		//returns the value of the data element
		
		return value_;
	}
	
	////////////////////////////////////////////////
	
	protected double value_; //element value
}
