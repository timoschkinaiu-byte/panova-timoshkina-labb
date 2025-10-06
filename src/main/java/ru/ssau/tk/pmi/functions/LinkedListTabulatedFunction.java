package ru.ssau.tk.pmi.functions;
import ru.ssau.tk.pmi.exceptions.ArrayIsNotSortedException;
import ru.ssau.tk.pmi.exceptions.DifferentLengthOfArraysException;
import ru.ssau.tk.pmi.exceptions.InterpolationException;
import java.util.Iterator;
public class LinkedListTabulatedFunction extends AbstractTabulatedFunction implements Insertable, Removable{

    static class Node {
        public double x;
        public double y;
        public Node next;
        public Node prev;
    }
    public Iterator<Point> iterator(){
        throw  new UnsupportedOperationException();
    }
    private int count = 0;
    private Node head = null;
    private void addNode(double x, double y) //добавление узла
    {
        Node newNode = new Node();
        newNode.x = x;
        newNode.y = y;
        if (head == null)
        {
            head = newNode;
            head.prev = head;
            head.next = head;
        }

        else{
            Node last = head.prev;
            last.next = newNode;
            newNode.prev = last;
            newNode.next = head;
            head.prev = newNode;
        }

        count += 1;
    }

    private Node getNode(int index){

        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("Индекс выходит за границы: " + index);
        }
        Node temp = head;
        int i = 0;
        while(i < index){
            temp = temp.next;
            i++;
        }
        return temp;
    }

    public LinkedListTabulatedFunction(double[] xValues, double [] yValues){
        if(xValues.length < 2){
            throw new IllegalArgumentException("длина меньше минимальной");
        }
        AbstractTabulatedFunction.checkLengthsIsTheSame(xValues, yValues);
        AbstractTabulatedFunction.checkSorted(xValues);
        for (int i = 0; i < xValues.length; i++){
            addNode(xValues[i], yValues[i]);
        }
    }

    public LinkedListTabulatedFunction(MathFunction source, double xFrom, double xTo, int count){

        if(count < 2){
            throw new IllegalArgumentException("длина меньше минимальной");
        }

        if (xFrom == xTo) {
            double yValue = source.apply(xFrom);
            for (int i = 0; i < count; i++) {
                addNode(xFrom, yValue);
            }
        }

        else {
            double step = (xTo - xFrom) / (count - 1);
            for (int i = 0; i < count; i++) {
                double x = xFrom + i * step;
                double y = source.apply(x);
                addNode(x, y);
            }
        }
    }

    public int getCount() {
        return count;
    }

    public double leftBound(){
        return head.x;
    }

    public double rightBound(){
        return head.prev.x;
    }

    public double getX(int index){
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("Индекс выходит за границы: " + index);
        }
        return getNode(index).x;
    }

    public double getY(int index){
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("Индекс выходит за границы: " + index);
        }
        return getNode(index).y;
    }

    public void setY(int index, double value){
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("Индекс выходит за границы: " + index);
        }
        getNode(index).y = value;
    }


    public int indexOfX(double x) {
        if (head == null) return -1;

        Node current = head;
        for (int i = 0; i < count; i++) {
            if (current.x == x) {
                return i;
            }
            current = current.next;
        }
        return -1;
    }


    public int indexOfY(double y) {
        if (head == null) return -1;

        Node current = head;
        for (int i = 0; i < count; i++) {
            if (current.y == y) {
                return i;
            }
            current = current.next;
        }
        return -1;
    }



    protected int floorIndexOfX(double x) {
        if (head == null) return 0;
        if (x < head.x){
            throw new IllegalArgumentException("x меньше левой границы: " + x);
        }
        if (x > head.prev.x) return count;

        Node current = head;
        for (int i = 0; i < count - 1; i++) {
            if (x >= current.x && x < current.next.x) {
                return i;
            }
            current = current.next;
        }
        return count - 1;
    }



    protected double interpolate(double x, int floorIndex) {
        if (count == 1) {
            return head.y;
        }

        Node leftNode = getNode(floorIndex);
        Node rightNode;
        if (floorIndex == count - 1) {
            rightNode = head;
        } else {
            rightNode = getNode(floorIndex + 1);
        }
        if(!(x>=leftNode.x && x<=rightNode.x)){
            throw new InterpolationException();
        }
        return interpolate(x, leftNode.x, rightNode.x, leftNode.y, rightNode.y);
    }


    protected double  extrapolateLeft(double x){

        return interpolate(x, head.x, head.next.x, head.y, head.next.y);
    }

    protected double extrapolateRight(double x) {

        Node last = head.prev;
        Node prevLast = last.prev;
        return interpolate(x, prevLast.x, last.x, prevLast.y, last.y);
    }

    @Override
    public double apply(double x) {
        if (x < leftBound()) {
            return extrapolateLeft(x);
        }

        if (x > rightBound()) {
            return extrapolateRight(x);
        }


        int exactIndex = indexOfX(x);
        if (exactIndex != -1) {
            return getY(exactIndex);
        }

        Node floorNode = floorNodeOfX(x);
        return interpolateFromNode(x, floorNode);
    }


    protected Node floorNodeOfX(double x) {
        if (head == null) return null;
        if (x < head.x){
            throw new IllegalArgumentException("x меньше левой границы: " + x);
        }
        if (x > head.prev.x) return head.prev;

        Node current = head;
        for (int i = 0; i < count; i++) {
            if (x >= current.x && (current.next == head || x < current.next.x)) {
                return current;
            }
            current = current.next;
        }
        return head.prev;
    }


    protected double interpolateFromNode(double x, Node leftNode) {

        Node rightNode = (leftNode.next == head) ? head.next : leftNode.next;
        return interpolate(x, leftNode.x, rightNode.x, leftNode.y, rightNode.y);
    }
    public void insert(double x, double y){
        if(head == null){
            addNode(x, y);
            return;
        }
        Node temp = head;
        do{
            if(x == temp.x){
                temp.y = y;
                return;
            }
            if(x>temp.x && x< temp.next.x ){
                Node newNode = new Node();
                newNode.x = x;
                newNode.y = y;
                Node node1 = temp;
                Node node2 = temp.next;
                node1.next = newNode;
                node2.prev = newNode;
                newNode.prev = node1;
                newNode.next = node2;
                count++;
                return;
            }
            if(x>temp.x && temp.next== head ){
                addNode(x,y);
                return;
            }

            if(x< temp.x && temp==head){
                Node newNode = new Node();
                newNode.x = x;
                newNode.y = y;
                Node prev = temp.prev;
                newNode.next = temp;
                newNode.prev = prev;
                prev.next = newNode;
                temp.prev = newNode;
                head = newNode;
                count++;
                return;
            }
            temp = temp.next;
        }while (temp!= head);
    }

    public void remove(int index){
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("Индекс выходит за границы: " + index);
        }

        Node temp = getNode(index);
        temp.prev.next = temp.next;
        temp.next.prev = temp.prev;
        if (index == 0){
            head = temp.next;
        }
        count --;
        temp.prev = null;
        temp.next = null;
    }

}
