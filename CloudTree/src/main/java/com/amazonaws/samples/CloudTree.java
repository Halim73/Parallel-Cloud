package com.amazonaws.samples;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

public class CloudTree<E> {
	private int numElements;
	private int nextNodeID;
	
	private AmazonDynamoDB db;
	private DynamoDB dynamo;
	private Table table;
	
	private HashMap<E,Integer>idToData;
	
	private String InOrder;
	private String PostOrder;
	private String PreOrder;
	private String tableState;
	private String tableName;
	
	private TreeSet<String>localTree = new TreeSet<>();
	
	public CloudTree(String name) {
		numElements = 0;
		nextNodeID = 0;
		
		tableName = name;
		
		ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider("Halim");
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (C:\\Users\\halim\\.aws\\credentials), and is in valid format.",
                    e);
        }
		
		db = AmazonDynamoDBClientBuilder.standard()
				.withCredentials(credentialsProvider)
				.withRegion("us-west-2")
				.build();
		
		CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(new KeySchemaElement().withAttributeName("ID").withKeyType(KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition().withAttributeName("ID").withAttributeType(ScalarAttributeType.S))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));
		
		TableUtils.createTableIfNotExists(db, createTableRequest);
		
        try {
        	TableUtils.waitUntilActive(db, tableName);
        }catch(InterruptedException e) {
        	e.printStackTrace();
        }
        
        DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
        TableDescription tableDescription = db.describeTable(describeTableRequest).getTable();
        tableState = tableDescription.toString();
        
        dynamo = new DynamoDB(db);
        table = dynamo.getTable(tableName);
        
        idToData = new HashMap<>();
		
        if(db.listTables().getTableNames().contains(tableName)){
        	nextNodeID = printInOrder().length()-(InOrder.length()/2-1)-2;
			for(String str:InOrder.split(",")) {
				localTree.add(str.replaceAll(",", ""));
			}
        	//System.out.println("Existing table "+InOrder+" with nextID at "+nextNodeID);
		}
	}
	
	public void insert(E data) {
		int currentID = 0;
		
		localTree.add(data.toString());
		
		Set<E>dataSet = new HashSet<>();
		List<Map<Integer,E>>childSet = new ArrayList<>(2);
		Map<Integer,E>children = new HashMap<>();
		
		dataSet.add(data);
		childSet.add(0,children);
		childSet.add(1,children);
		
		if(numElements == 0) {
			Item root = createItems(nextNodeID++,dataSet,childSet,true);
			table.putItem(root);
			idToData.put(data, nextNodeID-1);
			//System.out.println("The next id will be "+nextNodeID);
		}else {
			int childID = 0;
			while(true) {
	            childID = followChild(currentID,data);
	            
	            if(childID < 0)return;
	            
	            currentID = childID;
	            
	            break;
			}
			Item child = createItems(nextNodeID++,dataSet,childSet,true);
			table.putItem(child);
			
			addChild(currentID,nextNodeID-1,data);
			idToData.put(data, nextNodeID-1);
			//System.out.println("The next id will be "+nextNodeID);
		}
		numElements++;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addChild(int currentID,int child,E childData) {
		Item toAdd = retrieveItem(currentID);
		
		Map<String,E>childMap = new HashMap<>();
		childMap.put(Integer.toString(child), childData);
		
		List<Map<String,E>>list = toAdd.getList("child_set");
		List<Object>data = toAdd.getList("data_set");
		E obj = (E)data.get(0);
		
		ArrayList<Map<String,E>>aux = new ArrayList<>();
		
		Comparable test = (Comparable<E>)obj;
		Comparable test2 = (Comparable)childData;
		boolean goLeft = true;
		
		if(test instanceof Comparable) {
			if(test2 instanceof Comparable) {
				//System.out.println("comparing test1 = "+test+" test2 = "+test2);
				goLeft = (test.toString()).compareTo(test2.toString()) >= 0;
			}
		}
		
		
		if(goLeft) {
			list.set(0, childMap);
			aux = (ArrayList<Map<String,E>>)list;
			//System.out.println("went left");
		}else {
			list.set(1, childMap);
			aux = (ArrayList<Map<String,E>>)list;
			//System.out.println("went right");
		}
		
		Map<String,String>parentMap = new HashMap<>();
		parentMap.put("ID", Integer.toString(currentID));
		
		Map<String,Object>updateValues = new HashMap<>();
		updateValues.put(":child_set", aux);
		updateValues.put(":isLeaf", false);
		
		Map<String,String>updateNames = new HashMap<>();
		updateNames.put("#A", "child_set");
		updateNames.put("#B", "isLeaf");
		
		UpdateItemSpec update = new UpdateItemSpec()
				.withPrimaryKey("ID",Integer.toString(currentID))
				.withUpdateExpression("set #A = :child_set,#B = :isLeaf")
				.withNameMap(updateNames)
				.withValueMap(updateValues);
		
		//System.out.println("The item to update is "+toAdd.toJSONPretty());
		
		table.updateItem(update);
		//System.out.println("Attempted to insert child "+childMap.toString());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int followChild(int currentID,E data) {
		Item item = retrieveItem(currentID);
		
		if(item == null)return -1;
        
        List<E> theData = item.getList("data_set");
        List<Map<String,String>>theChildren = item.getList("child_set");
        boolean isLeaf = item.getBoolean("isLeaf");
        //System.out.println("is this a leaf "+item.getBoolean("isLeaf"));
        E obj = theData.get(0);
        
        if(isLeaf) {
        	 //System.out.println("found child to insert");
        	return currentID;
        }
        
        //System.out.println("the currently checked child is "+item.toJSONPretty());
        //boolean goLeft = ((Comparable)obj).compareTo((Comparable<E>)data) <= 0;
        
        Comparable test = (Comparable<E>)obj;
		Comparable test2 = (Comparable)data;
		boolean goLeft = true;
		
		if(test instanceof Comparable) {
			if(test2 instanceof Comparable) {
				//System.out.println("comparing "+test+" with "+test2);
				
				goLeft = (test.toString()).compareTo(test2.toString()) >= 0;
			}
		}
        
        if(goLeft) {
        	Map<String,String>childMap = theChildren.get(0);
        	if(!childMap.keySet().isEmpty()) {
        		String left = childMap.keySet().iterator().next();
            	return followChild(Integer.parseInt(left),data);
        	}
        }else {
        	Map<String,String>childMap = theChildren.get(1);
        	if(!childMap.keySet().isEmpty()) {
        		String right = childMap.keySet().iterator().next();
            	return followChild(Integer.parseInt(right),data);
        	}
        }
        return currentID;
	}
	
	public void delete(E data) {
		localTree.remove(data.toString());
		
		if(query(data)) {
			QuerySpec spec = new QuerySpec()
					.withAttributesToGet("ID");
			ItemCollection<QueryOutcome>list = table.query(spec);
			
			List<Object>dataSet = new LinkedList<>();
			dataSet.add((Object)data);
			
			Iterator<Item>it = list.iterator();
			Item item = null;
			
			while(it.hasNext()) {
				item = it.next();
				
				if(item.getList("data_set").equals(dataSet)) {
					DeleteItemSpec delSpec = new DeleteItemSpec()
							.withPrimaryKey("ID",item.get("ID"));
					table.deleteItem(delSpec);
					break;
				}
			}
		}
	}
	
	public void close() {
		try {
			table.delete();
			table.waitForDelete();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public boolean query(E data) {
		List<Object>dataSet = new LinkedList<>();
		dataSet.add((Object)data);
		
		if(idToData.containsKey(data)) {
			int id = idToData.get(data);
			
			Map<String,AttributeValue>valueMap = new HashMap<>();
			valueMap.put("ID",new AttributeValue(Integer.toString(id)));
			
			GetItemRequest request = new GetItemRequest()
					.withKey(valueMap)
					.withTableName(tableName);
			return db.getItem(request).getItem() != null;
		}else {
			String check = printInOrder();
			return check.contains(data.toString());
		}
		//return false;
	}
	
	public boolean checkIDExistence() {
		ScanRequest scanRequest = new ScanRequest(tableName)
        		.withTableName("ID");
        ScanResult scanResult = db.scan(scanRequest);
        
        for(Map<String,AttributeValue>element:scanResult.getItems()) {
        	Set<String>set = element.keySet();
        	for(String s:set) {
        		int id = Integer.parseInt(s);
        		
        		if(id == nextNodeID) return true;
        	}
        }
        return false;
	}
	
	public String printInOrder() {
		InOrder = "[";
		printInOrder(0);
		InOrder = InOrder.substring(0, InOrder.length()-1);
		InOrder += "]";
		
		return InOrder;
	}
	
	private void printInOrder(int id) {
		Item item = retrieveItem(id);
		
		if(item == null)return;
		
		if(item.getBoolean("isLeaf")) {
			InOrder += item.getList("data_set").get(0)+",";
			//System.out.print(item.getList("data_set").get(0)+",");
			return;
		}
		
		List<Map<String,String>>theChildren = item.getList("child_set");
		
		Map<String,String>leftChild = new HashMap<>();
		Map<String,String>rightChild = new HashMap<>();
		
		leftChild = theChildren.get(0);
		rightChild = theChildren.get(1);
		
		//System.out.println("the left child is "+leftChild.toString());
		//System.out.println("the right child is "+leftChild.toString());
		
		if(!leftChild.values().iterator().hasNext()
				&& !rightChild.values().iterator().hasNext())return;
		
		String left = "0";
		String right = "0";
		
		if(leftChild.keySet().iterator().hasNext()) {
			left = leftChild.keySet().iterator().next();
		}else {
			left = null;
		}
		
		if(rightChild.keySet().iterator().hasNext()) {
			right = rightChild.keySet().iterator().next();
		}else {
			right = null;
		}
		
		if(right == null && left ==null)return;
		
		if(left != null) {
			printInOrder(Integer.parseInt(left));
		}
		InOrder += item.getList("data_set").get(0)+",";
		//System.out.print(item.getList("data_set").get(0)+",");
		
		if(right != null) {
			printInOrder(Integer.parseInt(right));
		}
		
	}
	
	public String printPostOrder() {
		PostOrder = "[";
		printPostOrder(0);
		PostOrder = PostOrder.substring(0, PostOrder.length()-1);
		PostOrder += "]";
		
		return PostOrder;
	}
	
	private void printPostOrder(int id) {
		Item item = retrieveItem(id);
		
		if(item == null)return;
		
		if(item.getBoolean("isLeaf")) {
			PostOrder += item.getList("data_set").get(0)+",";
			//System.out.print(item.getList("data_set").get(0)+",");
			return;
		}
		
		List<Map<String,String>>theChildren = item.getList("child_set");
		
		Map<String,String>leftChild = new HashMap<>();
		Map<String,String>rightChild = new HashMap<>();
		
		leftChild = theChildren.get(0);
		rightChild = theChildren.get(1);
		
		//System.out.println("the left child is "+leftChild.toString());
		//System.out.println("the right child is "+leftChild.toString());
		
		if(!leftChild.values().iterator().hasNext()
				&& !rightChild.values().iterator().hasNext()) {
			System.out.print(item.getList("data_set").get(0)+",");
			return;
		}
		
		String left = "0";
		String right = "0";
		
		if(leftChild.keySet().iterator().hasNext()) {
			left = leftChild.keySet().iterator().next();
		}else {
			left = null;
		}
		
		if(rightChild.keySet().iterator().hasNext()) {
			right = rightChild.keySet().iterator().next();
		}else {
			right = null;
		}
		
		if(right == null && left ==null)return;
		
		if(left != null) {
			printPostOrder(Integer.parseInt(left));
		}
		
		if(right != null) {
			printPostOrder(Integer.parseInt(right));
		}
		PostOrder += item.getList("data_set").get(0)+",";
		//System.out.print(item.getList("data_set").get(0)+",");
	}
	
	public String printPreOrder() {
		PreOrder = "[";
		printPreOrder(0);
		PreOrder = PreOrder.substring(0, PreOrder.length()-1);
		PreOrder += "]";
		
		return PreOrder;
	}
	
	private void printPreOrder(int id) {
		Item item = retrieveItem(id);
		
		if(item == null)return;
		
		if(item.getBoolean("isLeaf")) {
			PreOrder += item.getList("data_set").get(0)+",";
			//System.out.print(item.getList("data_set").get(0)+",");
			return;
		}
		
		List<Map<String,String>>theChildren = item.getList("child_set");
		
		Map<String,String>leftChild = new HashMap<>();
		Map<String,String>rightChild = new HashMap<>();
		
		leftChild = theChildren.get(0);
		rightChild = theChildren.get(1);
		
		//System.out.println("the left child is "+leftChild.toString());
		//System.out.println("the right child is "+leftChild.toString());
		
		if(!leftChild.values().iterator().hasNext()
				&& !rightChild.values().iterator().hasNext()) {
			System.out.print(item.getList("data_set").get(0)+",");
			return;
		}
		
		String left = "0";
		String right = "0";
		
		if(leftChild.keySet().iterator().hasNext()) {
			left = leftChild.keySet().iterator().next();
		}else {
			left = null;
		}
		
		if(rightChild.keySet().iterator().hasNext()) {
			right = rightChild.keySet().iterator().next();
		}else {
			right = null;
		}
		PreOrder += item.getList("data_set").get(0)+",";
		//System.out.print(item.getList("data_set").get(0)+",");
		
		if(right == null && left ==null)return;
		
		if(left != null) {
			printPreOrder(Integer.parseInt(left));
		}
		
		if(right != null) {
			printPreOrder(Integer.parseInt(right));
		}
	}
	
	public Item retrieveItem(int id) {
		Item ret = null;
		try {
			ret = table.getItem("ID",Integer.toString(id));
		}catch(Exception e ) {
			e.printStackTrace();
		}
		//System.out.println("retrieved "+ret);
		return ret;
	}
	
	public Item createItems(int currentID,Set<E>data,List<Map<Integer,E>>children,boolean isLeaf) {
		Item item = new Item()
				.withPrimaryKey("ID",Integer.toString(currentID))
				.withKeyComponent("data_set",data)
				.withKeyComponent("child_set", children)
				.withKeyComponent("isLeaf", isLeaf);
		 //System.out.println("the item is "+item.toJSONPretty());
		return item;
	}
	
	public String printLocalTree() {
		return localTree.toString();
	}
	
	public String getDetails() {
		return tableState;
	}

}
