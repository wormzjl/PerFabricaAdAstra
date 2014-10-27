package org.pfaa.chemica.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.pfaa.geologica.Geologica;

import com.google.common.primitives.Doubles;

public class SimpleMixture implements Mixture {

	private List<MixtureComponent> components;
	
	protected SimpleMixture(List<MixtureComponent> components) {
		this.components = components;
	}

	public SimpleMixture(MixtureComponent... components) {
		this(Arrays.asList(components));
	}
	
	public SimpleMixture(IndustrialMaterial material, double weight) {
		this(new MixtureComponent(material, weight));
	}
	
	public SimpleMixture(IndustrialMaterial material) {
		this(new MixtureComponent(material, 1.0));
	}
	
	private double getTotalWeight() {
		double weight = 0;
		for (MixtureComponent comp : components) {
			weight += comp.weight;
		}
		return weight;
	}
	
	private double getTotalFluidWeight(Condition condition) {
		double weight = 0;
		for (MixtureComponent comp : components) {
			if (comp.getMaterial().getProperties(condition).phase != Phase.SOLID)
				weight += comp.weight;
		}
		return weight;
	}
	
	@Override
	public String name() {
		String name = "Mixture of ";
		double totalWeight = getTotalWeight();
		for (MixtureComponent comp : components) {
			name += (int)((comp.weight / totalWeight) * 100) + "% " + comp.material.name() + ", ";
		}
		return name.substring(0, name.length() - 1);
	}

	@Override
	public String getOreDictKey() {
		return null;
	}

	@Override
	public ConditionProperties getProperties(Condition condition) {
		if (this.components.size() == 0) {
			return null;
		}
		if (this.components.size() == 1) {
			return this.components.get(0).material.getProperties(condition);
		}
		double totalWeight = getTotalWeight();
		int r = 0, g = 0, b = 0, luminosity = 0;
		float health = 0, flammability = 0, instability = 0;
		double density = 0, opaqueWeight = 0;
		double[] phaseWeight = new double[Phase.values().length];
		for (MixtureComponent comp : this.components) {
			ConditionProperties props = comp.material.getProperties(condition);
			if (props == null)
				continue;
			double normWeight = comp.weight / totalWeight;
			r += props.color.getRed() * normWeight;
			b += props.color.getBlue() * normWeight;
			g += props.color.getGreen() * normWeight;
			density += props.density * normWeight;
			luminosity += props.luminosity * normWeight;
			health += props.hazard.health * normWeight;
			flammability += props.hazard.flammability * normWeight;
			instability += props.hazard.instability * normWeight;
			phaseWeight[props.phase.ordinal()] += comp.weight;
			if (props.opaque)
				opaqueWeight += normWeight; 
		}
		Phase phase = Phase.values()[Doubles.indexOf(phaseWeight, Doubles.max(phaseWeight))];
		return new ConditionProperties(phase, new Color(r, g, b), density,
				                       new Hazard(Math.round(health), Math.round(flammability), Math.round(instability)), 
				                       this.getViscosity(condition, density), luminosity,
				                       opaqueWeight > 0.5);
	}

	private static final double THOMAS_A = 0.00273;
	private static final double THOMAS_B = 16.6;

	private double getViscosity(Condition condition, double density) {
		double vbi = 0;
		double fluidWeight = getTotalFluidWeight(condition);
		// Chevron/Refutas method
		for (MixtureComponent comp : this.components) {
			ConditionProperties props = comp.material.getProperties(condition);
			if (props.phase != Phase.SOLID) {
				double normWeight = comp.weight / fluidWeight;
				double cSt = props.viscosity / props.density;
				vbi += normWeight * Math.log(cSt) / Math.log(1000 * cSt);
			}
		}
		double viscosity = Math.exp(vbi * Math.log(1000) / (1 - vbi)) * density;
		double solidFraction = 1 - fluidWeight / this.getTotalWeight();
		if (solidFraction > 0) {
			double relativeViscosity = 1 + 2.5*solidFraction + 10.05*Math.pow(solidFraction, 2.0) + 
					THOMAS_A*Math.exp(THOMAS_B*solidFraction); // Thomas (1965)
			viscosity *= relativeViscosity;
		}
		return viscosity;
	}

	@Override
	public List<MixtureComponent> getComponents() {
		return Collections.unmodifiableList(this.components);
	}
	
	@Override
	public Mixture mix(IndustrialMaterial material, double weight) {
		List<MixtureComponent> components = new ArrayList(this.components);
		components.add(new MixtureComponent(material, weight));
		return new SimpleMixture(components);
	}
}
