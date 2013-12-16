package com.sleepyduck.macdnotification.data;

import java.util.ArrayList;
import java.util.List;

import com.sleepyduck.macdnotification.data.xml.XMLElement;

public class Fund extends Symbol {
	private static final long serialVersionUID = -8656563746700038291L;
	private List<Symbol> mSymbols = new ArrayList<Symbol>();
	private List<Float> mWeights = new ArrayList<Float>();

	public Fund(String name) {
		super(name);
	}

	public Fund(XMLElement element) {
		super(element);
		XMLElement symbols = element.getElement("Symbols");
		if (symbols != null) {
			for (XMLElement xmlSymbol : symbols.getChildren()) {
				String name = xmlSymbol.getName();
				if (name.equals(Symbol.class.getSimpleName())) {
					Symbol symbol = new Symbol(xmlSymbol);
					mSymbols.add(symbol);
				} else if (name.equals(Fund.class.getSimpleName())) {
					Fund fund = new Fund(xmlSymbol);
					mSymbols.add(fund);
				}
				mWeights.add(Float.valueOf(xmlSymbol.getAttribute("weight", "1")));
			}
		}
	}

	@Override
	public void putAttributes(XMLElement element) {
		element.addAttribute("name", getName());
		XMLElement symbols = element.addChild(new XMLElement("Symbols"));
		for (int i = 0; i < Math.min(mSymbols.size(), mWeights.size()); ++i) {
			XMLElement xmlSymbol = mSymbols.get(i).toXMLElement();
			xmlSymbol.addAttribute("weight", Float.toString(mWeights.get(i)));
			symbols.addChild(xmlSymbol);
		}
	}

	@Override
	public void populateList(List<Symbol> list) {
		for (Symbol symbol : mSymbols) {
			symbol.populateList(list);
		}
	}

	@Override
	public float getMACD() {
		if (hasValidData()) {
			float macd = 0;
			float value = 0;
			float weight = 0;
			for (int i = 0; i < Math.min(mSymbols.size(), mWeights.size()); ++i) {
				Symbol symbol = mSymbols.get(i);
				macd += symbol.getMACD()/symbol.getValue()*mWeights.get(i);
				value += symbol.getValue()*mWeights.get(i);
				weight += mWeights.get(i);
			}
			return macd * value / weight / weight;
		} else {
			return super.getMACD();
		}
	}

	@Override
	public float getMACDOld() {
		if (hasValidData()) {
			float macd = 0;
			float value = 0;
			float weight = 0;
			for (int i = 0; i < Math.min(mSymbols.size(), mWeights.size()); ++i) {
				Symbol symbol = mSymbols.get(i);
				macd += symbol.getMACDOld()/symbol.getValueOld()*mWeights.get(i);
				value += symbol.getValueOld()*mWeights.get(i);
				weight += mWeights.get(i);
			}
			return macd * value / weight / weight;
		} else {
			return super.getMACDOld();
		}
	}

	@Override
	public CharSequence getDataText() {
		if (hasValidData()) {
			String text = String.format("MACD %2.2f (%2.2f)",
					getMACD(),
					getMACDOld());
			return text;
		} else {
			List<Symbol> allSymbols = asList();
			float count = 0;
			for (Symbol symbol : allSymbols) {
				if (symbol.hasValidData()) {
					count += 1;
				}
			}
			return "Loading: " + ((int)(count / allSymbols.size() * 100F)) + "%";
		}
	}

	@Override
	public boolean hasValidData() {
		if (mSymbols.size() == 0 || mWeights.size() == 0 || mSymbols.size() != mWeights.size())
			return false;
		for (Symbol symbol : mSymbols)
			if (symbol.getMACD() <= -99999f)
				return false;
		return true;
	}

	@Override
	public boolean hasDisplayName() {
		return true;
	}
}
