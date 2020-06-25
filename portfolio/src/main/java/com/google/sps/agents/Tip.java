package com.google.sps.agents;
 
// Imports the Google Cloud client library
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.io.IOException;
import java.util.Map;
import com.google.sps.data.Output;
import com.google.sps.agents.Agent;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import java.text.DecimalFormat;
 
/**
 * Tip Agent calculates tip for given parameters, only supports USD deimal formatting for now
 */
public class Tip implements Agent {
    private final String intentName;
  	private String searchText;
    private Double tipAmount;
    private Double tipAmountPerPerson;
    private String tipPercentageString = null;
    private Double tipPercentageDouble = null;
    private Double amountWithoutTip = null;
    private String currency = null;
    private String currencySymbol = null;
    private Double peopleNumber = null;
    
    public Tip(String intentName, Map<String, Value> parameters) {
      this.intentName = intentName;
      setParameters(parameters);
    }

    @Override 
    public void setParameters(Map<String, Value> parameters) {
        tipPercentageString = String.valueOf(parameters.get("tip-percentage").getStringValue());
        amountWithoutTip = parameters.get("amount-without-tip").getNumberValue();
        currency = parameters.get("currency").getStringValue();
        peopleNumber = parameters.get("people-number").getNumberValue();

        if (currency.equals("USD")) {
            currencySymbol = "$";
        }
    }
    
    @Override
    public String getOutput() {
        //Convert String to Doubles
        tipPercentageString = tipPercentageString.substring(0, tipPercentageString.length()-1);
        tipPercentageDouble = Double.valueOf(tipPercentageString);
        tipPercentageDouble = tipPercentageDouble/100;

        tipAmount = tipPercentageDouble * amountWithoutTip;
        DecimalFormat formatTipAmount = new DecimalFormat("#.##");   
        tipAmount = Double.valueOf(formatTipAmount.format(tipAmount)); 

        //Tip without number of people
        if (String.valueOf(peopleNumber).equals("0.0")) {
            return "The total tip is " + currencySymbol + String.valueOf(tipAmount);
        } else {
        //Tip with percentage and people
            tipAmountPerPerson = tipAmount / peopleNumber;
            DecimalFormat formatTipAmountPerPerson = new DecimalFormat("#.##");   
            tipAmountPerPerson = Double.valueOf(formatTipAmountPerPerson.format(tipAmountPerPerson));  
            return "The total tip is " + currencySymbol + String.valueOf(tipAmount) + ", coming out to " + currencySymbol + String.valueOf(tipAmountPerPerson) + " per person";
        }
    }
 
    @Override
    public String getDisplay() {
        return null;
    }
 
    @Override
    public String getRedirect() {
        return null;
    }
}