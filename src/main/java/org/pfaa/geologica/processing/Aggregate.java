package org.pfaa.geologica.processing;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import org.pfaa.chemica.model.Condition;
import org.pfaa.chemica.model.ConditionProperties;
import org.pfaa.chemica.model.Hazard;
import org.pfaa.chemica.model.IndustrialMaterial;
import org.pfaa.chemica.model.Mixture;
import org.pfaa.chemica.model.MixtureComponent;
import org.pfaa.chemica.model.Phase;

public interface Aggregate extends Mixture {
	
	public Aggregate mix(IndustrialMaterial material, double weight);
	
	public enum Aggregates implements Aggregate {
		SAND(new Color(237, 201, 175), 1.6), 
		GRAVEL(Color.gray, 1.2), 
		STONE(Color.gray, 1.4), 
		CLAY(Color.lightGray, 1.1), 
		DIRT(new Color(150, 75, 0), 1.1),
		OBSIDIAN(new Color(16, 16, 25), 2.5);

		private ConditionProperties properties;
		
		private Aggregates(Color color, double density) {
			this.properties = new ConditionProperties(Phase.SOLID, color, density, new Hazard());
		}
		
		@Override
		public String getOreDictKey() {
			return name();
		}

		@Override
		public ConditionProperties getProperties(Condition condition) {
			return this.properties;
		}

		@Override
		public List<MixtureComponent> getComponents() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Aggregate mix(IndustrialMaterial material, double weight) {
			return new SimpleAggregate(new MixtureComponent(this, 1.0), 
					                   new MixtureComponent(material, weight));
		}
	}
}
