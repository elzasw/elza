package cz.tacr.elza.bulkaction.generator;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;


/**
 * Class to bound level, its description items and parent
 * @author Petr Pytelka
 */
public class LevelWithItems {
	LevelWithItems parent;
    final ArrLevel level;
    
    /**
     * Description items
     */
    final List<ArrDescItem> descItems = new ArrayList<>();

    public LevelWithItems(final ArrLevel level) {
        this.level = level;
    }
    
    public LevelWithItems(final ArrLevel level, final LevelWithItems parentLevel) {
    	this.level = level;
		this.parent = parentLevel;
	}

	public LevelWithItems(final ArrLevel level, final LevelWithItems parentLevel, final List<ArrDescItem> items) {
    	this.level = level;
		this.parent = parentLevel;
		descItems.addAll(items);
	}

	public LevelWithItems getParent()
    {
    	return parent;
    }

	public ArrLevel getLevel() {
		return level;
	}

	public List<ArrDescItem> getDescItems() {
		return descItems;
	}
}