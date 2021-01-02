package de.dosmike.sponge.vshop.systems;

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.*;

public class ItemNBTCleaner {

	private static final Set<String[]> queries = new HashSet<>();

	/**
	 * for reloading
	 */
	public static void clear() {
		queries.clear();
	}

	public static void addQuery(String raw) {
		queries.add(raw.split("/"));
	}

	public static ItemStack filter(ItemStack item) {
		return ItemStack.builder().fromContainer(filter(item.toContainer().copy())).build();
	}

	public static ItemStackSnapshot filter(ItemStackSnapshot snapshot) {
		return ItemStack.builder().fromContainer(filter(snapshot.toContainer())).build().createSnapshot();
	}

	public static DataContainer filter(DataContainer container) {
		filter((DataView) container.copy());
		return container;
	}

	/**
	 * @return input for chaining
	 */
	public static DataView filter(DataView data) {
		for (String[] query : queries) {
			transform(data, query, 0);
		}
		return data;
	}

	/**
	 * I don't think DataViews are written using pure function, so i won't either
	 */
	private static void transform(DataView in, String[] query, int queryOffset) {
		String node = query[queryOffset];
		boolean asList = node.endsWith("[]");
		if (asList) node = node.substring(0, node.length() - 2);
		DataQuery next = DataQuery.of(node);

		if (queryOffset < query.length - 1) { // more to go
			if (in.contains(next)) {
				if (asList) {
					Optional<List<DataView>> children = in.getViewList(next);
					if (children.isPresent()) {
						List<DataView> transformed = new ArrayList<>(children.get().size());
						for (DataView child : children.get()) {
							transform(child, query, queryOffset + 1);
						}
						in.set(next, transformed);
					}
				} else {
					Optional<DataView> child = in.getView(next);
					child.ifPresent(dataView -> transform(dataView, query, queryOffset + 1));
				}
			}
		} else { //this is the last element
			if (in.contains(next))
				in.remove(next);
		}
	}
}
