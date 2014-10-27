package org.pfaa.geologica.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.pfaa.block.CompositeBlock;
import org.pfaa.chemica.model.Condition;
import org.pfaa.chemica.model.IndustrialMaterial;
import org.pfaa.geologica.GeoMaterial;
import org.pfaa.geologica.GeoMaterial.Strength;
import org.pfaa.geologica.Geologica;
import org.pfaa.geologica.processing.Aggregate;
import org.pfaa.geologica.processing.Aggregate.Aggregates;
import org.pfaa.geologica.processing.Crude;
import org.pfaa.geologica.processing.Ore;
import org.pfaa.geologica.processing.VanillaOre;
import org.pfaa.util.BlockWithMeta;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/* Rendering strategy:
 * If the GeoMaterial has a host, then we render the host texture, with an overlay (32x32).
 * We get the color from the GeoMaterial, and the overlay style depends on the
 * strength and block Material (sand ore is 'wavy') of the GeoMaterial.
 * For example: 
 * * weak => "fragmented" -- kind of like emerald ore, but not shiny
 * * medium => "diagonal scratches" -- like netherquartz
 * * strong => "horizontal bands" -- like vanilla ores.
 * BrickGeoBlock and BrokenGeoBlock will override to draw "crack" overlay.
 * The same thing needs to happen for walls, stairs, slabs, etc.
 */
public abstract class GeoBlock extends CompositeBlock implements GeoBlockAccessors {

	private List<GeoMaterial> materials = new ArrayList<GeoMaterial>();
	
	private Strength strength;
	private Class<? extends IndustrialMaterial> composition;

	private IIcon[] oreOverlayIcons = new IIcon[4];

	private static Map<GeoMaterial, BlockWithMeta<GeoBlock>> materialToNativeBlock = new HashMap();
	
	public GeoBlock(Strength strength, Class<? extends IndustrialMaterial> composition, Material material) {
		super(material);
		this.strength = strength;
		this.composition = composition;
		setCreativeTab(CreativeTabs.tabBlock);
		setGeoMaterials();
		setHardness(determineHardness());
		setResistance(determineResistance());
		setStepSound(determineStepSound());
		setHarvestLevel(determineHarvestTool(), determineHarvestLevel());
	}
	
	protected float determineResistance() {
		return Geologica.getConfiguration().getRockResistance(this.strength);
	}

	protected Block.SoundType determineStepSound() {
		Block.SoundType sound = null;
		if (blockMaterial == Material.rock) {
			sound = soundTypeStone;
		} else if (blockMaterial == Material.sand) {
			sound = soundTypeSand;
		} else if (blockMaterial == Material.clay) {
			sound = soundTypeGravel;
		}
		return sound;
	}
	
	protected float determineHardness() {
		float hardness = 0;
		if (blockMaterial == Material.rock)
			hardness = determineRockHardness();
		else if (blockMaterial == Material.clay)
			hardness = determineClayHardness();
		else if (blockMaterial == Material.sand)
			hardness = determineSandHardness();
		return hardness;
	}
 
	private float determineRockHardness() {
		return Geologica.getConfiguration().getRockHardness(this.strength);
	}

	private float determineClayHardness() {
		return 0.7F;
	}

	private float determineSandHardness() {
		return 0.6F;
	}

	public String determineHarvestTool() {
		if (blockMaterial == Material.rock)
			return "pickaxe";
		else return "shovel";
	}

	private int determineHarvestLevel() {
		return Geologica.getConfiguration().getHarvestLevel(this.composition, this.strength);
	}

	public boolean hasComposition(Class<? extends IndustrialMaterial> composition) {
		return composition.isAssignableFrom(this.composition);
	}

	private void setGeoMaterials() {
		List<GeoMaterial> materials = GeoMaterial.lookup(strength, composition, blockMaterial);
		if (materials.size() > 16) {
			materials = materials.subList(0, 16);
		}
		this.materials.clear();
		this.materials.addAll(materials);
	}
	
	public List<GeoMaterial> getGeoMaterials() {
		return Collections.unmodifiableList(materials);
	}

	/* (non-Javadoc)
	 * @see org.pfaa.geologica.block.GeoBlockAccessors#getSubstance(int)
	 */
	@Override
	public GeoMaterial getGeoMaterial(int meta) {
		return materials.get(meta);
	}

	public GeoMaterial getGeoMaterial(World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		return this.getGeoMaterial(meta);
	}
	
	/* (non-Javadoc)
	 * @see org.pfaa.geologica.block.GeoBlockAccessors#containsSubstance(org.pfaa.geologica.GeoSubstance)
	 */
	@Override
	public boolean containsGeoMaterial(GeoMaterial material) {
		return materials.contains(material);
	}
	
	@Override
	public int getMeta(GeoMaterial material) {
		return materials.indexOf(material);
	}
	
	@Override
	public int getMetaCount() {
		return materials.size();
	}

	@Override
	public String getBlockNameSuffix(int meta) {
		return getGeoMaterial(meta).getLowerName();
	}

	public ItemStack getItemStack(GeoMaterial material) {
		return new ItemStack(this, 1, getMeta(material));
	}

	public Strength getStrength() {
		return strength;
	}

	public Class<? extends IndustrialMaterial> getComposition() {
		return composition;
	}

	public Material getMaterial() {
		return blockMaterial;
	}

	@Override
	public boolean isReplaceableOreGen(World world, int x, int y, int z, Block target) {
		return Aggregate.class.isAssignableFrom(composition);
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
		GeoMaterial material = this.getGeoMaterial(metadata);
		ArrayList<ItemStack> drops = ChanceDropRegistry.instance().getDrops(material, world.rand, fortune);
		if (drops != null) {
			return drops;
		} else {
			return super.getDrops(world, x, y, z, metadata, fortune);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected IIcon getUnderlayIcon(IBlockAccess world, int x, int y, int z, int side) {
		int meta = world.getBlockMetadata(x, y, z);
		IndustrialMaterial host = this.getGeoMaterial(meta).getHost();
		if (host != null) {
			IIcon icon = getHostIcon(host, world, x, y, z);
			if (icon != null) {
				return icon;
			}
		}
		return this.getUnderlayIcon(side, meta);
	}
	
	@SideOnly(Side.CLIENT)
	protected IIcon getHostIcon(IndustrialMaterial host, IBlockAccess world, int x, int y, int z) {
		return null;
	}
	
	@Override
	protected int overlayColorMultiplier(int meta) {
		GeoMaterial material = this.getGeoMaterial(meta);
		if (material.getHost() != null) {
			return material.getProperties(Condition.STP).color.getRGB();
		}
		return super.overlayColorMultiplier(meta);
	}

	@Override
	protected boolean useMultipassRendering() {
		return this.needsHost();
	}

	private boolean needsHost() {
		return !Aggregate.class.isAssignableFrom(this.getComposition());
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister registry) {
		if (this.needsHost()) {
			// TODO: this needs to vary by position (1-4)
			this.oreOverlayIcons[0] = registry.registerIcon("geologica:" + this.getOverlayIconName());
		}
		super.registerBlockIcons(registry);
	}
	
	private String getMaterialName() {
		Material material = this.getMaterial();
		if (material == Material.rock) {
			return "Rock";
		} else if (material == Material.clay) {
			return "Clay";
		} else if (material == Material.sand) {
			return "Sand";
		}
		return null;
	}
	
	private String getOverlayIconName() {
		return this.getStrength().name().toLowerCase() + this.getCompositionName() + this.getMaterialName() + "Overlay"; 
	}

	private String getCompositionName() {
		return VanillaOre.class.isAssignableFrom(this.getComposition()) ? "VanillaOre" : "Ore";
	}

	@Override
	protected IIcon registerOverlayIcon(IIconRegister registry, int i) {
		if (this.getGeoMaterial(i).getHost() != null) {
			return this.oreOverlayIcons[0];
		}
		return super.registerOverlayIcon(registry, i);
	}
	
	private static Block getBlockForAggregate(Aggregate host) {
		if (host == Aggregates.STONE) {
			return Blocks.stone;
		} else if (host == Aggregates.SAND) {
			return Blocks.sand;
		} else if (host == Aggregates.CLAY) {
			return Blocks.clay;
		} else if (host == Aggregates.GRAVEL) {
			return Blocks.gravel;
		} else if (host == Aggregates.DIRT) {
			return Blocks.dirt;
		} else if (host == Aggregates.OBSIDIAN) {
			return Blocks.obsidian;
		}
		return null;
	}
	
	@Override
	protected IIcon registerUnderlayIcon(IIconRegister registry, int i) {
		IndustrialMaterial host = this.getGeoMaterial(i).getHost();
		if (host instanceof GeoMaterial) {
			GeoMaterial material = (GeoMaterial)host;
			BlockWithMeta<GeoBlock> blockWithMeta = getNative(material);
			return blockWithMeta.block.registerMetaIcon(registry, blockWithMeta.meta);
		} else if (host instanceof Aggregate) {
			Block block = getBlockForAggregate((Aggregate)host);
			if (block != null) {
				return block.getIcon(0, 0);
			}
		}
		return super.registerMetaIcon(registry, i);
	}

	public static GeoBlock registerNative(GeoBlock block) {
		List<GeoMaterial> materials = block.getGeoMaterials();
		int meta = 0;
		for (GeoMaterial key : materials) {
			materialToNativeBlock.put(key, new BlockWithMeta(block, meta++));
		}
		return block;
	}
	
	public static BlockWithMeta<GeoBlock> getNative(GeoMaterial material) {
		return materialToNativeBlock.get(material);
	}
}
