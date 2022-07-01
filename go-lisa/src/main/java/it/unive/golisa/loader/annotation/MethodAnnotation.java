package it.unive.golisa.loader.annotation;

import it.unive.lisa.program.annotations.Annotation;

/**
 * The class represents a method annotation.
 *
 * @author <a href="mailto:luca.olivieri@univr.it">Luca Olivieri</a>
 */
public class MethodAnnotation extends CodeAnnotation {

	private final String unit;
	private final String name;

	/**
	 * Builds an instance of method annotation.
	 * 
	 * @param annotation the annotation
	 * @param unit       the unit of method
	 * @param name       the method name
	 */
	public MethodAnnotation(Annotation annotation, String unit, String name) {
		super(annotation);
		this.unit = unit;
		this.name = name;
	}

	/**
	 * Yields the unit.
	 * 
	 * @return the unit
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Yields the method name.
	 * 
	 * @return the method name
	 */
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MethodAnnotation other = (MethodAnnotation) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (unit == null) {
			if (other.unit != null)
				return false;
		} else if (!unit.equals(other.unit))
			return false;
		return true;
	}

}
