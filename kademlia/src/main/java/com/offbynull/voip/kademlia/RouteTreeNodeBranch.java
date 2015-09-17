package com.offbynull.voip.kademlia;

import org.apache.commons.lang3.Validate;

final class RouteTreeNodeBranch implements RouteTreeBranch {

    private final RouteTreeNode node;

    public RouteTreeNodeBranch(RouteTreeNode node) {
        Validate.notNull(node);
        this.node = node;
    }

    @Override
    public BitString getPrefix() {
        return node.getPrefix();
    }

    @Override
    @SuppressWarnings("unchecked")
    public RouteTreeNode getItem() {
        return node;
    }
}
