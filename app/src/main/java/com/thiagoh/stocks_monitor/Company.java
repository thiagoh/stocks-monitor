package com.thiagoh.stocks_monitor;

/**
 * Created by thiago on 27/12/15.
 */
public class Company {

	private String name;
	private String symbol;

	public Company(String symbol, String name) {
		this.name = name;
		this.symbol = symbol;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Company company = (Company) o;

		return symbol.equals(company.symbol);

	}

	@Override
	public int hashCode() {
		return symbol.hashCode();
	}
}
