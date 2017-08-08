package gregtech.api.material.type;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import gregtech.api.material.Element;
import gregtech.api.material.MaterialIconSet;
import gregtech.api.interfaces.IMaterialHandler;
import gregtech.api.objects.MaterialStack;
import gregtech.api.util.GTControlledRegistry;
import gregtech.api.util.GT_Log;

import java.util.ArrayList;

import static gregtech.api.GT_Values.M;

public abstract class Material implements Comparable<Material> {

	public static GTControlledRegistry<Material> MATERIAL_REGISTRY = new GTControlledRegistry<>();
	public static GTControlledRegistry<IMaterialHandler> MATERIAL_HANDLER_REGISTRY = new GTControlledRegistry<>();

	/**
	 * Initializes material and also creates fluid instances
	 */
	public static void init() {
		MATERIAL_HANDLER_REGISTRY.freezeRegistry();
		for (String name : MATERIAL_HANDLER_REGISTRY.getKeys()) {
			IMaterialHandler materialHandler = MATERIAL_HANDLER_REGISTRY.getObject(name);
			try {
				materialHandler.onMaterialsInit();
			} catch (Throwable exception) {
				GT_Log.logger.warn("Caught exception while trying to init materials by handler " +
						MATERIAL_HANDLER_REGISTRY.getFullNameForObject(materialHandler), exception);
			}
		}

		MATERIAL_REGISTRY.freezeRegistry();
		for(String name : MATERIAL_REGISTRY.getKeys()) {
			Material material = MATERIAL_REGISTRY.getObject(name);
			material.initMaterial(name);
		}
	}

	public static final class MatFlags {

		/**
		 * Enables electrolyzer decomposition recipe generation
		 */
		public static final long DECOMPOSITION_BY_ELECTROLYZING = createFlag(0);

		/**
		 * Enables centrifuge decomposition recipe generation
		 */
		public static final long DECOMPOSITION_BY_CENTRIFUGING = createFlag(1);

        /**
         * Add to material if it has constantly burning aura
         */
        public static final long BURNING = createFlag(7);

		/**
		 * Add to material if it is some kind of flammable
		 */
		public static final long FLAMMABLE = createFlag(2);

		/**
		 * Add to material if it is some kind of explosive
		 */
		public static final long EXPLOSIVE = createFlag(4);

		/**
		 * Add to material to disable it's unification fully
		 */
		public static final long NO_UNIFICATION = createFlag(5);

		/**
		 * Add to material if any of it's items cannot be recycled to get scrap
		 */
		public static final long NO_RECYCLING = createFlag(6);


		public static long createFlag(int id) {
			return (long) Math.pow(2, id);
		}

	}

	/**
	 * Default Localized Material Name
	 * Since material amount is huge, default localization entries for them are auto generated
	 */
	public final String defaultLocalName;

	/**
	 * Color of material in RGB format
	 */
	public final int materialRGB;

	/**
	 * Chemical formula of this material
	 */
	public final String chemicalFormula;

	/**
	 * Icon set for this material meta-items generation
	 */
	public final MaterialIconSet materialIconSet;

	/**
	 * List of this material component
	 */
	public final ImmutableList<MaterialStack> materialComponents;

	/**
	 * List of materials to re-register this material into
	 * As example, all Iron materials are re-registered into generic material AnyIron
	 */
	public final ArrayList<MarkerMaterial> oreReRegistrations = new ArrayList<>();

	/**
	 * Generation flags of this material
	 * @see MatFlags
	 * @see DustMaterial.MatFlags
	 */
	protected long materialGenerationFlags;

	/**
	 * Element of this material consist of
	 */
	public final Element element;

	private String calculateChemicalFormula() {
	    if(element != null) {
	        return element.name();
        }
        if(!materialComponents.isEmpty()) {
	        StringBuilder components = new StringBuilder();
	        for(MaterialStack component : materialComponents)
	            components.append(component.toString());
	        return components.toString();
        }
        return "";
    }

	public Material(int metaItemSubId, String name, String defaultLocalName, int materialRGB, MaterialIconSet materialIconSet, ImmutableList<MaterialStack> materialComponents, long materialGenerationFlags, Element element) {
		this.defaultLocalName = defaultLocalName;
		this.materialRGB = materialRGB;
		this.materialIconSet = materialIconSet;
		this.materialComponents = materialComponents;
		this.materialGenerationFlags = verifyMaterialBits(materialGenerationFlags);
		this.element = element;
        this.chemicalFormula = calculateChemicalFormula();
        if(metaItemSubId > -1) {
        	MATERIAL_REGISTRY.register(metaItemSubId, name, this);
		} else MATERIAL_REGISTRY.putObject(name, this);
	}

	protected long verifyMaterialBits(long materialBits) {
		return materialBits;
	}

	public void add(long... materialGenerationFlags) {
		long combined = 0;
		for (long materialGenerationFlag : materialGenerationFlags) {
			combined |= materialGenerationFlag;
		}
		this.materialGenerationFlags |= verifyMaterialBits(combined);
	}

	public boolean hasFlag(long generationFlag) {
		return (materialGenerationFlags & generationFlag) != 0;
	}

	protected void initMaterial(String name) {

	}

	public boolean isRadioactive() {
		if (element != null)
			return element.halfLifeSeconds >= 0;
		for (MaterialStack material : materialComponents)
			if (material.material.isRadioactive()) return true;
		return false;
	}

	public long getProtons() {
		if (element != null)
			return element.getProtons();
		if (materialComponents.size() <= 0)
			return Element.Tc.getProtons();
		long totalProtons = 0, totalAmount = 0;
		for (MaterialStack material : materialComponents) {
			totalAmount += material.amount;
			totalProtons += material.amount * material.material.getProtons();
		}
		return (getDensity() * totalProtons) / (totalAmount * M);
	}

	public long getNeutrons() {
		if (element != null)
			return element.getNeutrons();
		if (materialComponents.size() <= 0)
			return Element.Tc.getNeutrons();
		long totalProtons = 0, totalAmount = 0;
		for (MaterialStack material : materialComponents) {
			totalAmount += material.amount;
			totalProtons += material.amount * material.material.getNeutrons();
		}
		return (getDensity() * totalProtons) / (totalAmount * M);
	}

	public long getMass() {
		if (element != null)
			return element.getMass();
		if (materialComponents.size() <= 0)
			return Element.Tc.getMass();
		long totalProtons = 0, totalAmount = 0;
		for (MaterialStack material : materialComponents) {
			totalAmount += material.amount;
			totalProtons += material.amount * material.material.getMass();
		}
		return (getDensity() * totalProtons) / (totalAmount * M);
	}

	public long getDensity() {
		return M;
	}

	public String toCamelCaseString() {
		return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, toString());
	}

	@Override
	public String toString() {
		return MATERIAL_REGISTRY.getNameForObject(this);
	}

	@Override
	public int compareTo(Material material) {
		String anotherId = MATERIAL_REGISTRY.getNameForObject(material);
		return MATERIAL_REGISTRY.getNameForObject(this).compareTo(anotherId);
	}

}