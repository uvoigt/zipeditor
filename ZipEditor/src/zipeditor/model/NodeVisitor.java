/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

public abstract class NodeVisitor {

	protected boolean canceled;

	protected boolean propagateResult;

	public abstract Object visit(Node node, Object argument);
}
