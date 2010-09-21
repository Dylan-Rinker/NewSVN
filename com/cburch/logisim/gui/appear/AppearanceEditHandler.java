/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.appear;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.cburch.draw.actions.ModelDeleteHandleAction;
import com.cburch.draw.actions.ModelInsertHandleAction;
import com.cburch.draw.actions.ModelReorderAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.Selection;
import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.util.MatchingSet;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.gui.main.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Project;

public class AppearanceEditHandler extends EditHandler
		implements SelectionListener, PropertyChangeListener, CanvasModelListener {
	private AppearanceCanvas canvas;
	
	AppearanceEditHandler(AppearanceCanvas canvas) {
		this.canvas = canvas;
		canvas.getSelection().addSelectionListener(this);
		CanvasModel model = canvas.getModel();
		if (model != null) model.addCanvasModelListener(this);
		canvas.addPropertyChangeListener(Canvas.MODEL_PROPERTY, this);
	}
	
	@Override
	public void computeEnabled() {
		Project proj = canvas.getProject();
		Circuit circ = canvas.getCircuit();
		Selection sel = canvas.getSelection();
		boolean selEmpty = sel.isEmpty();
		boolean canChange = proj.getLogisimFile().contains(circ);
		boolean clipExists = !Clipboard.isEmpty();
		boolean canRaise;
		boolean canLower;
		if (!selEmpty && canChange) {
			canRaise = true;
			canLower = true;
		} else {
			canRaise = false;
			canLower = false;
		}
		boolean canAddCtrl = false;
		boolean canRemCtrl = false;
		Handle handle = sel.getSelectedHandle();
		if (handle != null && canChange) {
			CanvasObject o = handle.getObject();
			canAddCtrl = o.canInsertHandle(handle.getLocation()) != null;
			canRemCtrl = o.canDeleteHandle(handle.getLocation()) != null;
		}
		
		setEnabled(LogisimMenuBar.CUT, !selEmpty && canChange);
		setEnabled(LogisimMenuBar.COPY, !selEmpty);
		setEnabled(LogisimMenuBar.PASTE, canChange && clipExists);
		setEnabled(LogisimMenuBar.DELETE, !selEmpty && canChange);
		setEnabled(LogisimMenuBar.DUPLICATE, !selEmpty && canChange);
		setEnabled(LogisimMenuBar.SELECT_ALL, true);
		setEnabled(LogisimMenuBar.RAISE, canRaise);
		setEnabled(LogisimMenuBar.LOWER, canLower);
		setEnabled(LogisimMenuBar.ADD_CONTROL, canAddCtrl);
		setEnabled(LogisimMenuBar.REMOVE_CONTROL, canRemCtrl);
	}
	
	@Override
	public void cut() {
		if (!canvas.getSelection().isEmpty()) {
			canvas.getProject().doAction(ClipboardActions.cut(canvas));
		}
	}
	
	@Override
	public void copy() {
		if (!canvas.getSelection().isEmpty()) {
			canvas.getProject().doAction(ClipboardActions.copy(canvas));
		}
	}
	
	@Override
	public void paste() {
		List<CanvasObject> clip = Clipboard.get().getObjects();
		List<CanvasObject> add = new ArrayList<CanvasObject>(clip.size());
		for (CanvasObject o : clip) {
			add.add(o.clone());
		}
		if (add.isEmpty()) return;
		
		// find how far we have to translate shapes so that at least one of the
		// pasted shapes doesn't match what's already in the model
		Collection<CanvasObject> raw = canvas.getModel().getObjectsFromBottom(); 
		MatchingSet<CanvasObject> cur = new MatchingSet<CanvasObject>(raw);
		while (true) {
			// if any shapes in "add" aren't in canvas, we are done
			boolean allMatch = true;
			for (CanvasObject o : add) {
				if (!cur.contains(o)) {
					allMatch = false;
					break;
				}
			}
			if (!allMatch) break;
			
			// otherwise translate everything by 10 pixels and repeat test
			for (CanvasObject o : add) {
				o.translate(10, 10);
			}
		}	
			
		canvas.getProject().doAction(new SelectionAction(canvas,
				Strings.getter("pasteClipboardAction"), null, add, add));
	}
	
	@Override
	public void delete() {
		Selection sel = canvas.getSelection();
		int n = sel.getSelected().size();
		List<CanvasObject> select = new ArrayList<CanvasObject>(n);
		List<CanvasObject> remove = new ArrayList<CanvasObject>(n);
		for (CanvasObject o : sel.getSelected()) {
			if (o.canRemove()) {
				remove.add(o);
			} else {
				select.add(o);
			}
		}
		
		if (!remove.isEmpty()) {
			canvas.getProject().doAction(new SelectionAction(canvas,
				Strings.getter("deleteSelectionAction"), remove, null, select));
		}
	}
	
	@Override
	public void duplicate() {
		Selection sel = canvas.getSelection();
		int n = sel.getSelected().size();
		List<CanvasObject> select = new ArrayList<CanvasObject>(n);
		List<CanvasObject> clones = new ArrayList<CanvasObject>(n);
		for (CanvasObject o : sel.getSelected()) {
			if (o.canRemove()) {
				CanvasObject copy = o.clone();
				copy.translate(10, 10);
				clones.add(copy);
				select.add(copy);
			} else {
				select.add(o);
			}
		}
		
		if (!clones.isEmpty()) {
			canvas.getProject().doAction(new SelectionAction(canvas,
				Strings.getter("duplicateSelectionAction"), null, clones, select));
		}
	}
	
	@Override
	public void selectAll() {
		Selection sel = canvas.getSelection();
		sel.setSelected(canvas.getModel().getObjectsFromBottom(), true);
		canvas.repaint();
	}
	
	@Override
	public void raise() {
		ModelReorderAction act = ModelReorderAction.createRaise(canvas.getModel(),
				canvas.getSelection().getSelected());
		if (act != null) {
			canvas.doAction(act);
		}
	}
	
	@Override
	public void lower() {
		ModelReorderAction act = ModelReorderAction.createLower(canvas.getModel(),
				canvas.getSelection().getSelected());
		if (act != null) {
			canvas.doAction(act);
		}
	}

	@Override
	public void addControlPoint() {
		Selection sel = canvas.getSelection();
		Handle handle = sel.getSelectedHandle();
		canvas.doAction(new ModelInsertHandleAction(canvas.getModel(), handle));
	}
	
	@Override
	public void removeControlPoint() {
		Selection sel = canvas.getSelection();
		Handle handle = sel.getSelectedHandle();
		canvas.doAction(new ModelDeleteHandleAction(canvas.getModel(), handle));
	}


	public void selectionChanged(SelectionEvent e) {
		computeEnabled();
	}

	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();
		if (prop.equals(Canvas.MODEL_PROPERTY)) {
			CanvasModel oldModel = (CanvasModel) e.getOldValue();
			if (oldModel != null) {
				oldModel.removeCanvasModelListener(this);
			}
			CanvasModel newModel = (CanvasModel) e.getNewValue();
			if (newModel != null) {
				newModel.addCanvasModelListener(this);
			}
		}
	}

	public void modelChanged(CanvasModelEvent event) {
		computeEnabled();
	}
}
