/*
 * $HeadURL$
 * $LastChangedBy$
 * $Date$
 * $Revision$
 */
package com.blueprintit.menuedit;

import java.applet.AppletContext;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.io.Writer;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.blueprintit.swim.PageBrowser;
import com.blueprintit.swim.Request;
import com.blueprintit.swim.SwimInterface;
import com.blueprintit.xui.InterfaceEvent;
import com.blueprintit.xui.InterfaceListener;

public class EditorUI implements InterfaceListener
{
	private class MenuItem implements MutableTreeNode
	{
		private MenuItem parent;
		private Vector subitems = new Vector();
		private String text = "Root";
		private String url;
		private String resource;
		private boolean useURL=true;
		private boolean hasLink=false;
		
		public MenuItem(MenuItem parent)
		{
			this.parent=parent;
		}
		
		public MenuItem(MenuItem parent, Element element)
		{
			this(parent);
			if (element.getTagName().equals("item"))
			{
				if (element.hasAttribute("resource"))
				{
					resource=element.getAttribute("resource");
					useURL=false;
					hasLink=true;
				}
				else if (element.hasAttribute("url"))
				{
					url=element.getAttribute("url");
					useURL=true;
					hasLink=true;
				}
				text=element.getAttribute("text");
				Node node = element.getFirstChild();
				while (node!=null)
				{
					if (node instanceof Element)
					{
						if (((Element)node).getTagName().equals("menu"))
						{
							processMenuElement((Element)node);
						}
					}
					node=node.getNextSibling();
				}
			}
			else if (element.getTagName().equals("menu"))
			{
				processMenuElement(element);
			}
		}
		
		private void processMenuElement(Element element)
		{
			Node node = element.getFirstChild();
			while (node!=null)
			{
				if (node instanceof Element)
				{
					if (((Element)node).getTagName().equals("item"))
					{
						subitems.add(new MenuItem(this,(Element)node));
					}
				}
				node=node.getNextSibling();
			}
		}
		
		public Element getElement(Document doc)
		{
			Element el = doc.createElement("item");
			el.setAttribute("text",text);
			if (hasLink)
			{
				if (useURL)
				{
					el.setAttribute("url",url);
				}
				else
				{
					el.setAttribute("resource",resource);
				}
			}
			if (subitems.size()>0)
			{
				el.appendChild(getMenuElement(doc));
			}
			return el;
		}
		
		public Element getMenuElement(Document doc)
		{
			Element el = doc.createElement("menu");
			Iterator it = subitems.iterator();
			while (it.hasNext())
			{
				MenuItem child = (MenuItem)it.next();
				el.appendChild(child.getElement(doc));
			}
			return el;
		}
		
		public TreePath getTreePath()
		{
			if (parent==null)
			{
				return new TreePath(this);
			}
			else
			{
				return parent.getTreePath().pathByAddingChild(this);
			}
		}
		
		public boolean getHasLink()
		{
			return hasLink;
		}
		
		public void setHasLink(boolean value)
		{
			hasLink=value;
		}
		
		public boolean getUseURL()
		{
			return useURL;
		}
		
		public void setUseURL(boolean value)
		{
			useURL=value;
		}
		
		public void setText(String value)
		{
			this.text=value;
		}
		
		public String getText()
		{
			return text;
		}
		
		public void setURL(String value)
		{
			this.url=value;
		}
		
		public String getURL()
		{
			return url;
		}
		
		public void setResource(String value)
		{
			this.resource=value;
		}
		
		public String getResource()
		{
			return resource;
		}
		
		public String toString()
		{
			return text;
		}
		
		public int getChildCount()
		{
			return subitems.size();
		}

		public boolean getAllowsChildren()
		{
			return true;
		}

		public boolean isLeaf()
		{
			return getChildCount()==0;
		}

		public Enumeration children()
		{
			return subitems.elements();
		}

		public TreeNode getParent()
		{
			return parent;
		}

		public TreeNode getChildAt(int childIndex)
		{
			return (TreeNode)subitems.get(childIndex);
		}

		public int getIndex(TreeNode node)
		{
			return subitems.indexOf(node);
		}

		public void removeFromParent()
		{
			parent=null;			
		}

		public void remove(int index)
		{
			subitems.remove(index);
		}

		public void setUserObject(Object object)
		{
			text=object.toString();
		}

		public void remove(MutableTreeNode node)
		{
			subitems.remove(node);
		}

		public void setParent(MutableTreeNode newParent)
		{
			parent=(MenuItem)newParent;
		}

		public void insert(MutableTreeNode child, int index)
		{
			subitems.add(index,(MenuItem)child);
		}
	}
	
	private class JTreeDnDHandler implements DragGestureListener, DropTargetListener, DragSourceListener
	{
		private Logger log = Logger.getLogger(this.getClass());
		
		private TreePath source;
		private JTree tree;
		
		public JTreeDnDHandler(JTree tree)
		{
			this.tree=tree;
			DragSource source = new DragSource();
			source.addDragSourceListener(this);
			source.createDefaultDragGestureRecognizer(tree,DnDConstants.ACTION_MOVE,this);
			new DropTarget(tree,DnDConstants.ACTION_MOVE,this,true);
		}
		
		private boolean isAcceptableDrag(Point loc)
		{
			if (source!=null)
			{
				TreePath target = tree.getClosestPathForLocation(loc.x,loc.y);
				if (!source.isDescendant(target))
				{
					if (((TreeNode)source.getLastPathComponent()).getParent()!=target.getLastPathComponent())
						return true;
				}
			}
			return false;
		}
		
		public void dragGestureRecognized(DragGestureEvent dge)
		{
			source = tree.getSelectionPath();
			if (source.getPathCount()>1)
			{
				StringSelection text = new StringSelection(source.toString());
				dge.startDrag(DragSource.DefaultMoveNoDrop,text);
			}
			else
			{
				source=null;
			}
		}

		public void dragEnter(DropTargetDragEvent dtde)
		{
			if (isAcceptableDrag(dtde.getLocation()))
			{
				dtde.acceptDrag(DnDConstants.ACTION_MOVE);
			}
			else
			{
				dtde.rejectDrag();
			}
		}

		public void dragOver(DropTargetDragEvent dtde)
		{
			dragEnter(dtde);
		}

		public void dropActionChanged(DropTargetDragEvent dtde)
		{
			dragEnter(dtde);
		}

		public void drop(DropTargetDropEvent dtde)
		{
			log.debug("drop");
			if (source!=null)
			{
				TreePath target = tree.getClosestPathForLocation(dtde.getLocation().x,dtde.getLocation().y);
				if (!source.isDescendant(target))
				{
					MutableTreeNode node = (MutableTreeNode)source.getLastPathComponent();
					MutableTreeNode dest = (MutableTreeNode)target.getLastPathComponent();
					DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
					model.removeNodeFromParent(node);
					model.insertNodeInto(node,dest,dest.getChildCount());
					node.setParent(dest);
					dtde.dropComplete(true);
				}
			}
			dtde.rejectDrop();
		}

		public void dragExit(DropTargetEvent dte)
		{
		}

		public void dragEnter(DragSourceDragEvent dsde)
		{
			dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
		}

		public void dragOver(DragSourceDragEvent dsde)
		{
		}

		public void dropActionChanged(DragSourceDragEvent dsde)
		{
		}

		public void dragDropEnd(DragSourceDropEvent dsde)
		{
		}

		public void dragExit(DragSourceEvent dse)
		{
			dse.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
		}
	}
	
	private Logger log = Logger.getLogger(this.getClass());
	
	private SwimInterface swim;
	private String menu;
	private AppletContext context;
	private URL cancelURL;
	private URL commitURL;
	private MenuItem root;
	
	public JTree tree;
	public JButton btnAdd;
	public JButton btnDel;
	public JButton btnUp;
	public JButton btnDown;
	public JRadioButton radioNoLink;
	public JRadioButton radioInternal;
	public JRadioButton radioExternal;
	public JButton btnInternal;
	public JTextField textExternal;
	
	public Action saveAction = new AbstractAction("Save") {
		public void actionPerformed(ActionEvent e)
		{
			try
			{
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document list=builder.newDocument();
				list.appendChild(root.getMenuElement(list));

				TransformerFactory tFactory = TransformerFactory.newInstance();
			  Transformer transformer = tFactory.newTransformer();

				Request request = swim.getRequest(menu);
				request.addParameter("version","temp");
				Writer writer = request.openWriter();

				DOMSource source = new DOMSource(list);
			  StreamResult result = new StreamResult(writer);
			  transformer.setOutputProperty(OutputKeys.INDENT,"yes");
			  transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
			  transformer.transform(source, result);
			  
				writer.close();
			}
			catch (Exception ex)
			{
				log.error("Unable to generate document",ex);
			}
		}
	};

	public Action cancelAction = new AbstractAction("Cancel") {
		public void actionPerformed(ActionEvent e)
		{
			context.showDocument(cancelURL);
		}
	};

	public Action menuAddAction = new AbstractAction("Add") {
		public void actionPerformed(ActionEvent e)
		{
			MenuItem parent = (MenuItem)tree.getSelectionPath().getLastPathComponent();
			MenuItem item = new MenuItem(parent);
			item.setText("New Menu Item");
			DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
			model.insertNodeInto(item,parent,parent.getChildCount());
			tree.setSelectionPath(item.getTreePath());
		}
	};

	public Action menuDeleteAction = new AbstractAction("Delete") {
		public void actionPerformed(ActionEvent e)
		{
			MenuItem item = (MenuItem)tree.getSelectionPath().getLastPathComponent();
			DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
			model.removeNodeFromParent(item);
			tree.clearSelection();
		}
	};

	public Action menuMoveUpAction = new AbstractAction("Move Up") {
		public void actionPerformed(ActionEvent e)
		{			
			MenuItem item = (MenuItem)tree.getSelectionPath().getLastPathComponent();
			DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
			int pos = item.getParent().getIndex(item)-1;
			model.removeNodeFromParent(item);
			model.insertNodeInto(item,(MutableTreeNode)item.getParent(),pos);
			tree.setSelectionPath(item.getTreePath());
		}
	};

	public Action menuMoveDownAction = new AbstractAction("Move Down") {
		public void actionPerformed(ActionEvent e)
		{			
			MenuItem item = (MenuItem)tree.getSelectionPath().getLastPathComponent();
			DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
			int pos = item.getParent().getIndex(item)+1;
			model.removeNodeFromParent(item);
			model.insertNodeInto(item,(MutableTreeNode)item.getParent(),pos);
			tree.setSelectionPath(item.getTreePath());
		}
	};

	public Action browseInternalAction = new AbstractAction() {
		public void actionPerformed(ActionEvent e)
		{			
			MenuItem item = (MenuItem)tree.getSelectionPath().getLastPathComponent();
			PageBrowser dlg = swim.getPageBrowser();
			String page = dlg.choosePage(item.getResource());
			if ((page!=null)&&(page.length()>0))
			{
				item.setResource(page);
			}
		}
	};

	private void radioSelectionChanged()
	{
		btnInternal.setEnabled(radioInternal.isSelected()&&radioInternal.isEnabled());
		textExternal.setEnabled(radioExternal.isSelected()&&radioExternal.isEnabled());
	}
	
	public Action radioInternalAction = new AbstractAction() {
		public void actionPerformed(ActionEvent e)
		{
			radioSelectionChanged();
			if (radioInternal.isSelected())
			{
				MenuItem item = (MenuItem)tree.getSelectionPath().getLastPathComponent();
				PageBrowser dlg = swim.getPageBrowser();
				String page = dlg.choosePage();
				if ((page!=null)&&(page.length()>0))
				{
					item.setResource(page);
					item.setHasLink(true);
					item.setUseURL(false);
				}
				else
				{
					if (item.getHasLink())
					{
						radioExternal.setSelected(true);
					}
					else
					{
						radioNoLink.setSelected(true);
					}
				}
			}
		}
	};

	public Action radioExternalAction = new AbstractAction() {
		public void actionPerformed(ActionEvent e)
		{
			radioSelectionChanged();
			if (radioExternal.isSelected())
			{
				MenuItem item = (MenuItem)tree.getSelectionPath().getLastPathComponent();
				item.setHasLink(true);
				item.setUseURL(true);
			}
		}
	};

	public Action radioNoLinkAction = new AbstractAction() {
		public void actionPerformed(ActionEvent e)
		{
			radioSelectionChanged();
			if (radioNoLink.isSelected())
			{
				MenuItem item = (MenuItem)tree.getSelectionPath().getLastPathComponent();
				item.setHasLink(false);
			}
		}
	};

	public EditorUI(AppletContext context, SwimInterface swim, String path, URL cancel, URL commit)
	{
		this.context=context;
		this.swim=swim;
		this.menu=path;
		cancelURL=cancel;
		commitURL=commit;
	}
	
	public void interfaceCreated(InterfaceEvent ev)
	{
		try
		{
			Request request = swim.getRequest("view",menu);
			request.addParameter("version","temp");
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document list=builder.parse(request.encode().toString());
			root = new MenuItem(null,list.getDocumentElement());
			DefaultTreeModel model = new DefaultTreeModel(root);
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			tree.setModel(model);
			new JTreeDnDHandler(tree);
			tree.addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent e)
				{
					TreePath path = e.getNewLeadSelectionPath();
					boolean enabled=((path!=null)&&(path.getPathCount()>1));
					btnAdd.setEnabled((path!=null)&&(path.getPathCount()>=1));
					btnDel.setEnabled(enabled);
					tree.setEditable(enabled);
					
					if (enabled)
					{
						MenuItem item = (MenuItem)path.getLastPathComponent();

						if (item.getParent()!=null)
						{
							btnUp.setEnabled(item.getParent().getIndex(item)>0);
							btnDown.setEnabled(item.getParent().getIndex(item)<(item.getParent().getChildCount()-1));
						}
						else
						{
							btnDown.setEnabled(false);
							btnUp.setEnabled(false);
						}
						
						radioInternal.setEnabled(true);
						radioExternal.setEnabled(true);
						radioNoLink.setEnabled(true);

						textExternal.setText(item.getURL());
						if (item.getHasLink())
						{
							if (item.getUseURL())
							{
								radioExternal.setSelected(true);
							}
							else
							{
								radioInternal.setSelected(true);
							}
						}
						else
						{
							radioNoLink.setSelected(true);
						}
					}
					else
					{
						btnDown.setEnabled(false);
						btnUp.setEnabled(false);
						radioInternal.setEnabled(false);
						radioExternal.setEnabled(false);
						radioNoLink.setEnabled(false);
						textExternal.setEnabled(false);
						btnInternal.setEnabled(false);
					}
					radioSelectionChanged();
				}
			});
		}
		catch (Exception e)
		{
			log.error("Unable to load menu",e);
		}
	}
}
