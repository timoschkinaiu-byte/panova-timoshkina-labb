package functions;

public class LinkedListTabulatedFunction extends AbstractTabulatedFunction{

    static class Node {
        public double x;
        public double y;
        public Node next;
        public Node prev;
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
        Node temp = head;
        int i = 0;
        while(i < index){
            temp = temp.next;
            i++;
        }
        return temp;
    }

    public LinkedListTabulatedFunction(double[] xValues, double [] yValues){
        for (int i = 0; i < xValues.length; i++){
            addNode(xValues[i], yValues[i]);
        }
    }

    public LinkedListTabulatedFunction(MathFunction source, double xFrom, double xTo, int count){

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
        return getNode(index).x;
    }

    public double getY(int index){
        return getNode(index).y;
    }

    public void setY(int index, double value){
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
        if (x < head.x) return 0;
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


    /*protected double interpolate(double x, int floorIndex){
        if (count == 1){
            return head.y;
        }

        Node leftNode = getNode(floorIndex);
        Node rightNode = leftNode.next;

        if (rightNode == head) {
            rightNode = head.next;
        }

        return interpolate(x, leftNode.x, rightNode.x, leftNode.y, rightNode.y);
    }*/


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

        return interpolate(x, leftNode.x, rightNode.x, leftNode.y, rightNode.y);
    }


    protected double  extrapolateLeft(double x){
        if (count < 2) {
            return head.y;
        }
        return interpolate(x, head.x, head.next.x, head.y, head.next.y);
    }

    protected double extrapolateRight(double x) {
        if (count < 2) {
            return head.y;
        }
        Node last = head.prev;
        Node prevLast = last.prev;
        return interpolate(x, prevLast.x, last.x, prevLast.y, last.y);
    }
}
