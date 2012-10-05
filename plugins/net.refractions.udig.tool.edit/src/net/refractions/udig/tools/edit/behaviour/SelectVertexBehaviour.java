/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004, Refractions Research Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package net.refractions.udig.tools.edit.behaviour;

import java.util.ArrayList;
import java.util.List;

import net.refractions.udig.project.command.UndoableComposite;
import net.refractions.udig.project.command.UndoableMapCommand;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;
import net.refractions.udig.tools.edit.EditPlugin;
import net.refractions.udig.tools.edit.EditState;
import net.refractions.udig.tools.edit.EditToolHandler;
import net.refractions.udig.tools.edit.EventBehaviour;
import net.refractions.udig.tools.edit.EventType;
import net.refractions.udig.tools.edit.commands.SelectVertexCommand;
import net.refractions.udig.tools.edit.commands.SetEditStateCommand;
import net.refractions.udig.tools.edit.commands.SelectVertexCommand.Type;
import net.refractions.udig.tools.edit.preferences.PreferenceUtil;
import net.refractions.udig.tools.edit.support.EditBlackboard;
import net.refractions.udig.tools.edit.support.EditGeom;
import net.refractions.udig.tools.edit.support.Point;
import net.refractions.udig.tools.edit.support.Selection;

/**
 * The basic select vertext behaviour.
 *  <ul>
 * <li>if no modifiers clears selection and adds selected vertex</li>
 * <li>if shift down adds selected vertex</li>
 * <li>if ctrl down removes vertex if in selection or adds selected vertex if not</li>
 * <li>sets Edit State to Modified</li>
 * </ul>
 *
 * <p>Requirements: * <ul> * <li>eventType RELEASED</li>
 * <li>handler has currentGeom</li>
 * <li>edit state is modified or NONE</li>
 * <li>only one of ctrl or shift is</li>
 * <li>Mouse is over a vertex</li>
 *</ul> * </p> * <p>Action: * <ul> * <li>if no modifiers clears selection and adds selected vertex</li>
 * <li>if shift down adds selected vertex</li>
 * <li>if ctrl down removes vertex if in selection or adds selected vertex if not</li>
 * <li>sets Edit State to Modified</li>
 *</ul> * </p>
 * @author jones
 * @since 1.1.0
 */
public class SelectVertexBehaviour implements EventBehaviour {
    
    public boolean isValid( EditToolHandler handler, MapMouseEvent e, EventType eventType ) {
       boolean currentGeomNotNull = handler.getCurrentGeom()!=null;
        boolean eventTypePressed = eventType==EventType.RELEASED;
        boolean twoModifiersDown = e.isShiftDown()&&e.isControlDown();
        boolean singleModifierDown = !(twoModifiersDown);
        boolean altUp=!e.isAltDown();
        boolean button1Changed=(e.button^MapMouseEvent.BUTTON1)==0;
        
        if ( !(currentGeomNotNull && eventTypePressed&& singleModifierDown&&altUp&&button1Changed) )
            return false;
        EditGeom geom = handler.getCurrentGeom();
        return geom.hasVertex(geom.getEditBlackboard().overVertex(Point.valueOf(e.x, e.y), PreferenceUtil.instance().getVertexRadius()));
    }

    public UndoableMapCommand getCommand( EditToolHandler handler, MapMouseEvent e, EventType eventType ) {
        if( !isValid(handler, e, eventType) )
            throw new IllegalArgumentException("Not valid state", new Exception()); //$NON-NLS-1$

        EditBlackboard editBlackboard = handler.getEditBlackboard(handler.getEditLayer());
        Point point = handler.getEditBlackboard(handler.getEditLayer()).overVertex(Point.valueOf(e.x,e.y), PreferenceUtil.instance().getVertexRadius());
        List<EditGeom> geoms = null;
        if( point != null )
            geoms= editBlackboard.getGeoms(point.getX(),point.getY());
        else{
            EditPlugin.trace(EditPlugin.SELECTION, "VertexSelectorBehaviour: Not over vertex (" //$NON-NLS-1$
                    +e.x+","+e.y+")", null); //$NON-NLS-1$ //$NON-NLS-2$
        }
        Selection selection = editBlackboard.getSelection();
        List<UndoableMapCommand> commands=new ArrayList<UndoableMapCommand>();
        if( e.isShiftDown() ){
            if( geoms!=null && geoms.contains(handler.getCurrentGeom()) 
                    && !editBlackboard.getSelection().contains(point) ){
                commands.add( new SelectVertexCommand(editBlackboard, point, Type.ADD ) );
            }
        } else if (e.isControlDown()) {
            if (geoms != null && geoms.contains(handler.getCurrentGeom())) {
                if( selection.contains(point) )
                    commands.add( new SelectVertexCommand(editBlackboard, point, Type.REMOVE) );
                else{
                    commands.add( new SelectVertexCommand(editBlackboard, point, Type.ADD) );                    
                }
            }
        }else{
            if( selection.size()!=1 || !selection.contains(point) )
                commands.add(  new SelectVertexCommand(editBlackboard, point, Type.SET) );
        }
        if( geoms!=null && geoms.contains(handler.getCurrentGeom()) ){
            if( handler.getCurrentState()==EditState.NONE){
                commands.add( new SetEditStateCommand( handler, EditState.MODIFYING ) );
            }
        }
        if( commands.size() == 0)
            return null;
        else
            return new UndoableComposite(commands);
    }

    public void handleError( EditToolHandler handler, Throwable error, UndoableMapCommand command ) {
        EditPlugin.log("", error); //$NON-NLS-1$
    }

}
