package org.pfaa.geologica.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;

import org.pfaa.block.CompositeBlockAccessors;
import org.pfaa.geologica.block.SlabBlock;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SlabItem extends ItemSlab {
	public SlabItem(Block block) {
		super(block, ((SlabBlock)block).getSingleSlab(), 
			         ((SlabBlock)block).getDoubleSlab(), 
			         ((SlabBlock)block).isDoubleSlab());
	}
}
