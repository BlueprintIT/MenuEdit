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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import com.blueprintit.errors.ErrorReporter;
import com.blueprintit.swim.Request;
import com.blueprintit.swim.SwimInterface;
import com.blueprintit.xui.InterfaceEvent;
import com.blueprintit.xui.InterfaceListener;

public class EditorUI implements InterfaceListener
{
	private abstract class Item implements MutableTreeNode, Comparable
	{
		protected CategoryItem parent;
		protected String name;
		
		private Item(String name)
		{
			this.name=name;
		}
		
		private Item(Element el)
		{
			this.name = el.getText().trim();
		}
		
		public abstract Item deepClone();
		
		protected void cloneInto(Item item)
		{
		}
		
		protected abstract String getType();
		
		public String toString()
		{
			return name;
		}
		
		public Element getElement()
		{
			Element el = new Element(getType());
			el.addContent(this.toString());
			return el;
		}
		
		public int compareTo(Object obj)
		{
			return this.toString().compareTo(obj.toString());
		}
		
		public TreePath getTreePath()
		{
			if (parent!=null)
			{
				return parent.getTreePath().pathByAddingChild(this);
			}
			return new TreePath(this);
		}
		
		public void insert(MutableTreeNode node, int pos)
		{
		}

		public void remove(int pos)
		{
		}

		public void remove(MutableTreeNode node)
		{
		}

		public void setUserObject(Object obj)
		{
		}

		public void removeFromParent()
		{
			this.parent.remove(this);
			this.parent=null;
		}

		public void setParent(MutableTreeNode node)
		{
			this.parent=(CategoryItem)node;
		}

		public TreeNode getChildAt(int arg0)
		{
			return null;
		}

		public int getChildCount()
		{
			return 0;
		}

		public TreeNode getParent()
		{
			return parent;
		}

		public int getIndex(TreeNode arg0)
		{
			return -1;
		}

		public boolean getAllowsChildren()
		{
			return false;
		}

		public boolean isLeaf()
		{
			return true;
		}

		public Enumeration children()
		{
			return null;
		}
	}
	
	private class CategoryItem extends Item
	{
		private List children = new ArrayList();
		private boolean sorted = false;
		
		CategoryItem(String name)
		{
			super(name);
		}
		
		CategoryItem(String name, boolean sorted)
		{
			super(name);
			this.sorted=sorted;
		}
		
		CategoryItem(Element el)
		{
			super(el);
			parseChildren(el);
		}
		
		public void parseChildren(Element el)
		{
			Iterator it = el.getChildren().iterator();
			while (it.hasNext())
			{
				Element ch = (Element)it.next();
				if (ch.getName()=="category")
				{
					Item newitem = new CategoryItem(ch);
					append(newitem);
				}
				else if (ch.getName()=="page")
				{
					Item newitem = new PageItem(ch);
					append(newitem);
				}
				else if (ch.getName()=="link")
				{
					Item newitem = new LinkItem(ch);
					append(newitem);
				}
			}
		}
		
		public Element getElement()
		{
			Element el = parent.getElement();
			return el;
		}
		
		public Item deepClone()
		{
			CategoryItem clone = new CategoryItem(name);
			parent.cloneInto(clone);
			Iterator it = children.iterator();
			while (it.hasNext())
			{
				Item item = (Item)it.next();
				clone.append(item.deepClone());
			}
			return clone;
		}
		
		public void setName(String name)
		{
			this.name=name;
		}
		
		protected String getType()
		{
			return "category";
		}
		
		private int getInsertPos(MutableTreeNode node)
		{
			int pos = Collections.binarySearch(children,node);
			if (pos<0)
			{
				pos=-(pos+1);
			}
			return pos;
		}
		
		public int append(MutableTreeNode node)
		{
			int pos = children.size();
			if (sorted)
				pos=getInsertPos(node);
			this.insert(node,pos);
			return pos;
		}
		
		public void insert(MutableTreeNode node, int pos)
		{
			children.add(pos,node);
			node.setParent(this);
		}

		public void remove(int pos)
		{
			Item node = (Item)children.remove(pos);
			node.setParent(null);
		}

		public void remove(MutableTreeNode node)
		{
			children.remove(node);
			node.setParent(null);
		}

		public void setUserObject(Object obj)
		{
		}

		public void removeFromParent()
		{
			this.parent.remove(this);
			this.parent=null;
		}

		public void setParent(MutableTreeNode node)
		{
			this.parent=(CategoryItem)node;
		}

		public TreeNode getChildAt(int pos)
		{
			return (TreeNode)children.get(pos);
		}

		public int getChildCount()
		{
			return children.size();
		}

		public int getIndex(TreeNode node)
		{
			return children.indexOf(node);
		}

		public boolean getAllowsChildren()
		{
			return true;
		}

		public boolean isLeaf()
		{
			return false;
		}

		public Enumeration children()
		{
			return (new Vector(children)).elements();
		}
	}
	
	private class PageItem extends Item
	{
		protected String path;
		
		PageItem(String name, String path)
		{
			super(name);
			this.path=path;
			addToCache();
		}
		
		PageItem(Element el)
		{
			super(el);
			this.path=el.getAttributeValue("path");
			addToCache();
		}
		
		private void addToCache()
		{
			if (pagecache.containsKey(path))
			{
				log.debug("Duplicate page added");
				((Set)pagecache.get(path)).add(this);
			}
			else
			{
				Set col = new HashSet();
				col.add(this);
				pagecache.put(path,col);
			}
		}
		
		public Element getElement()
		{
			Element el = parent.getElement();
			el.setAttribute("path",path);
			return el;
		}

		public Item deepClone()
		{
			PageItem clone = new PageItem(name,path);
			parent.cloneInto(clone);
			return clone;
		}
		
		protected String getType()
		{
			return "page";
		}
		
		public String getPath()
		{
			return path;
		}
	}
	
	private class LinkItem extends Item
	{
		protected String path;
		
		LinkItem(String name, String path)
		{
			super(name);
			this.path=path;
		}
		
		LinkItem(Element el)
		{
			super(el);
			this.path=el.getAttributeValue("path");
		}
		
		public Element getElement()
		{
			Element el = parent.getElement();
			el.setAttribute("path",path);
			return el;
		}

		protected String getType()
		{
			return "link";
		}

		public void setName(String name)
		{
			this.name=name;
		}
		
		public Item deepClone()
		{
			LinkItem clone = new LinkItem(name,path);
			parent.cloneInto(clone);
			return clone;
		}
		
		public String getPath()
		{
			return path;
		}
		
		public void setPath(String path)
		{
			this.path=path;
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
			source.createDefaultDragGestureRecognizer(tree,DnDConstants.ACTION_COPY_OR_MOVE,this);
			new DropTarget(tree,DnDConstants.ACTION_COPY_OR_MOVE,this,true);
		}
		
		private boolean isAcceptableDrag(int operation, Point loc)
		{
			if (source==null)
				return false;
			
			TreePath target = tree.getClosestPathForLocation(loc.x,loc.y);
			if (source.isDescendant(target))
				return false;

			if (target.getPathComponent(1)==unused)
				return false;
			
			if ((operation==DnDConstants.ACTION_COPY)&&(source.getPathComponent(1)==unused))
				return false;

			return true;
		}
		
		public void dragGestureRecognized(DragGestureEvent dge)
		{
			source = tree.getSelectionPath();
			if ((source!=null)&&(source.getPathCount()>2))
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
			if (isAcceptableDrag(dtde.getDropAction(),dtde.getLocation()))
			{
				dtde.acceptDrag(dtde.getDropAction());
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
				MutableTreeNode node = (MutableTreeNode)source.getLastPathComponent();
				MutableTreeNode dest = (MutableTreeNode)target.getLastPathComponent();
				int insertpos=dest.getChildCount();
				if (!dest.getAllowsChildren())
				{
					insertpos=dest.getParent().getIndex(dest);
					dest=(MutableTreeNode)dest.getParent();
				}
				else if (node.getParent()==dest)
				{
					insertpos--;
					if ((insertpos==dest.getIndex(node))&&(dtde.getDropAction()==DnDConstants.ACTION_MOVE))
					{
						dtde.rejectDrop();
						return;
					}
				}
				DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
				if (dtde.getDropAction()==DnDConstants.ACTION_MOVE)
				{
					model.removeNodeFromParent(node);
					model.insertNodeInto(node,dest,insertpos);
				}
				else if (dtde.getDropAction()==DnDConstants.ACTION_COPY)
				{
					Item newnode = ((Item)node).deepClone();
					model.insertNodeInto(newnode,dest,insertpos);
				}
				dtde.dropComplete(true);
			}
			dtde.rejectDrop();
		}

		public void dragExit(DropTargetEvent dte)
		{
		}

		public void dragEnter(DragSourceDragEvent dsde)
		{
			switch (dsde.getDropAction())
			{
				case DnDConstants.ACTION_COPY:
					dsde.getDragSourceContext().setCursor(DragSource.DefaultCopyDrop);
					break;
				case DnDConstants.ACTION_MOVE:
					dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
					break;
				default:
					dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
			}
		}

		public void dragOver(DragSourceDragEvent dsde)
		{
			dragEnter(dsde);
		}

		public void dropActionChanged(DragSourceDragEvent dsde)
		{
			dragEnter(dsde);
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
	private AppletContext context;
	private URL cancelURL;
	private URL commitURL;
	private CategoryItem root;
	private CategoryItem mainroot;
	private CategoryItem unused;
	private Map pagecache = new HashMap();
	
	public JTree tree;
	public JPopupMenu popup;
	
	private boolean saveWorking()
	{
		try
		{
			Request request = swim.getRequest(resource);
			request.addParameter("version","temp");
			Writer writer = request.openWriter();

			Document doc = new Document();
			doc.setRootElement(mainroot.getElement());
			XMLOutputter outputter = new XMLOutputter();
			outputter.getFormat().setOmitEncoding(true);
			outputter.getFormat().setOmitDeclaration(true);
			outputter.output(doc,writer);
		  
			writer.close();
			return true;
		}
		catch (Exception ex)
		{
			if (ex.getMessage().startsWith("Server returned HTTP response code: 409 for URL"))
			{
				JOptionPane.showMessageDialog(null,"Another user has taken over editing of this resource, you will be unable to save your changes.","Resource Locked",JOptionPane.ERROR_MESSAGE);
			}
			else if (ex.getMessage().startsWith("Server returned HTTP response code: 401 for URL"))
			{
				JOptionPane.showMessageDialog(null,"You are no longer logged in to the server, your session probably expired.","Authentication Required",JOptionPane.ERROR_MESSAGE);
			}
			else
			{
				log.error("Unable to generate document",ex);
				ErrorReporter.sendErrorReport(
						"Unable to save","The file could not be saved, probably because the server is currently unavailable.",
						"Swim","MenuEdit","Could not save",ex);
			}
			return false;
		}
	}
	
	public Action commitAction = new AbstractAction("Save") {
		public void actionPerformed(ActionEvent e)
		{
			if (saveWorking())
				context.showDocument(commitURL);
		}
	};

	public Action cancelAction = new AbstractAction("Cancel") {
		public void actionPerformed(ActionEvent e)
		{
			context.showDocument(cancelURL);
		}
	};

	public Action menuRenameAction = new AbstractAction("Rename...") {
		public void actionPerformed(ActionEvent e)
		{
			Item item = (Item)tree.getSelectionPath().getLastPathComponent();
			if ((item instanceof LinkItem)||(item instanceof CategoryItem))
			{
				String text = JOptionPane.showInputDialog("Enter a new name for this category:",item.toString());
				if (text!=null)
				{
					if (item instanceof CategoryItem)
						((CategoryItem)item).setName(text);
					else
						((LinkItem)item).setName(text);
					DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
					model.nodeChanged(item);
				}
			}
		}
	};

	public Action menuAddCategoryAction = new AbstractAction("Add Category") {
		public void actionPerformed(ActionEvent e)
		{
			String text = JOptionPane.showInputDialog("Enter a name for this category:","");
			if (text!=null)
			{
				CategoryItem parent = (CategoryItem)tree.getSelectionPath().getLastPathComponent();
				CategoryItem item = new CategoryItem(text);
				DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
				model.insertNodeInto(item,parent,parent.getChildCount());
				tree.setSelectionPath(item.getTreePath());
				tree.scrollPathToVisible(item.getTreePath());
			}
		}
	};

	public boolean claimUnused(Item item)
	{
		if (item instanceof PageItem)
		{
			PageItem page = (PageItem)item;
			Set set = (Set)pagecache.get(page.getPath());
			if (set.size()==1)
			{
				log.debug("Found a soon to be uncategorised page");
				DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
				model.removeNodeFromParent(page);
				int pos = unused.append(page);
				model.nodesWereInserted(unused,new int[] {pos});
				return true;
			}
			else
			{
				log.debug("Removing an already linked page");
				set.remove(page);
			}
		}
		else if (item instanceof CategoryItem)
		{
			CategoryItem cat = (CategoryItem)item;
			Enumeration en = cat.children();
			while (en.hasMoreElements())
			{
				claimUnused((Item)en.nextElement());
			}
		}
		return false;
	}
	
	public Action menuDeleteAction = new AbstractAction("Remove from Category") {
		public void actionPerformed(ActionEvent e)
		{
			Item item = (Item)tree.getSelectionPath().getLastPathComponent();
			if (!claimUnused(item))
			{
				DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
				model.removeNodeFromParent(item);
			}
			tree.clearSelection();
		}
	};

	public Action menuMoveUpAction = new AbstractAction("Move Up") {
		public void actionPerformed(ActionEvent e)
		{			
			Item item = (Item)tree.getSelectionPath().getLastPathComponent();
			DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
			int pos = item.getParent().getIndex(item)-1;
			MutableTreeNode parent = (MutableTreeNode)item.getParent();
			model.removeNodeFromParent(item);
			model.insertNodeInto(item,parent,pos);
			tree.setSelectionPath(item.getTreePath());
			tree.scrollPathToVisible(item.getTreePath());
		}
	};

	public Action menuMoveDownAction = new AbstractAction("Move Down") {
		public void actionPerformed(ActionEvent e)
		{			
			Item item = (Item)tree.getSelectionPath().getLastPathComponent();
			DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
			int pos = item.getParent().getIndex(item)+1;
			MutableTreeNode parent = (MutableTreeNode)item.getParent();
			model.removeNodeFromParent(item);
			model.insertNodeInto(item,parent,pos);
			tree.setSelectionPath(item.getTreePath());
			tree.scrollPathToVisible(item.getTreePath());
		}
	};

	private String resource;

	public EditorUI(AppletContext context, SwimInterface swim, String resource, URL cancel, URL commit)
	{
		this.context=context;
		this.swim=swim;
		this.resource=resource;
		cancelURL=cancel;
		commitURL=commit;
	}
	
	public void loadMenu(String menu) throws Exception
	{
		Request request = swim.getRequest("view",menu);
		SAXBuilder builder = new SAXBuilder();
		Document list = builder.build(request.encode());
		root = new CategoryItem("Website");
		mainroot = new CategoryItem("Website");
		unused = new CategoryItem("Uncategorised Pages",true);
		root.append(mainroot);
		root.append(unused);
		mainroot.parseChildren(list.getRootElement().getChild("tree"));
		unused.parseChildren(list.getRootElement().getChild("pages"));
		DefaultTreeModel model = new DefaultTreeModel(root);	
		tree.setModel(model);
		tree.setSelectionRow(0);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setScrollsOnExpand(false);
		tree.putClientProperty("JTree.lineStyle", "None");
		
		Enumeration en = unused.children();
		while (en.hasMoreElements())
		{
			PageItem page = (PageItem)en.nextElement();
			Set set = (Set)pagecache.get(page.getPath());
			if (set.size()>1)
			{
				set.remove(page);
				model.removeNodeFromParent(page);
			}
		}
	}
	
	public void interfaceCreated(InterfaceEvent ev)
	{
		try
		{
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			new JTreeDnDHandler(tree);
			tree.addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent e)
				{
					TreePath path = e.getNewLeadSelectionPath();
					boolean enabled=((path!=null)&&(path.getPathCount()>2));
					if (enabled&&(path.getLastPathComponent() instanceof PageItem))
						enabled=false;
					tree.setEditable(enabled);
					menuRenameAction.setEnabled(enabled);
					if (path==null)
					{
						menuDeleteAction.setEnabled(false);
						menuMoveUpAction.setEnabled(false);
						menuMoveDownAction.setEnabled(false);
					}
					else
					{
						TreeNode item = (TreeNode)path.getLastPathComponent();
						if (path.getPathCount()<=2)
						{
							menuDeleteAction.setEnabled(false);
							menuMoveUpAction.setEnabled(false);
							menuMoveDownAction.setEnabled(false);
						}
						else
						{
							menuDeleteAction.setEnabled(true);
							TreeNode parent = item.getParent();
							menuMoveUpAction.setEnabled(parent.getIndex(item)>0);
							menuMoveDownAction.setEnabled((parent.getIndex(item)+1<parent.getChildCount()));
						}
						
						if (item instanceof CategoryItem)
						{
							menuAddCategoryAction.setEnabled(true);
						}
						else
						{
							menuAddCategoryAction.setEnabled(false);
						}
					}
				}
			});
			tree.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e)
				{
					maybeShowPopup(e);
				}

				public void mouseReleased(MouseEvent e)
				{
					maybeShowPopup(e);
				}
				
				private void maybeShowPopup(MouseEvent e)
				{
					if (e.isPopupTrigger())
					{
						TreePath target = tree.getClosestPathForLocation(e.getX(),e.getY());
						tree.setSelectionPath(target);
						if (target.getPathComponent(1)!=unused)
							popup.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			});
			loadMenu(resource);
		}
		catch (Exception e)
		{
			log.error("Unable to load menu",e);
			ErrorReporter.sendErrorReport(
					"Error loading content","The menu to be edited could not be loaded. The server could be down or misconfigured.",
					"Swim","MenuEdit","Could not load menu",e);
		}
	}
}
