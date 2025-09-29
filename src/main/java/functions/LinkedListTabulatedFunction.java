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

    public int indexOfX(double x){
        int i = 0;
        Node temp = head;
        do{
            temp = temp.next;
            i++;
        } while(x != temp.x && i <= count);
        if (i == count){
            return -1;
        }
        else {
            return i;
        }
    }

    public int indexOfY(double y){
        int i = 0;
        Node temp = head;
        do{
            temp = temp.next;
            i++;
        } while(y != temp.y && i <= count);
        if (i == count){
            return -1;
        }
        else {
            return i;
        }
    }

    protected int floorIndexOfX(double x){
        if (head == null) return 0;
        if (x < head.x) return 0;
        if (x > head.prev.x) return count;

        if (indexOfX(x) == -1){
            int i = 0;
            Node temp = head;
            do{
                temp = temp.next;
                i++;
            } while(temp.x < x);
            return i;
        }
        else{
            return indexOfX(x);
        }
    }


    protected double interpolate(double x, int floorIndex){
        if (count == 1) {
            return head.y;
        }

        Node leftNode = getNode(floorIndex);
        Node rightNode = leftNode.next;

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
