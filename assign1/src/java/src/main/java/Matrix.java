import static java.lang.Math.min;


public class Matrix {

    public static void main(String[] args) {


        //lineMult(3000);
        //onMult(3000);
        blockMul(3000, 128);
    }


    public static void onMult(int size){
        double[] pha = new double[size*size];
        double[] phb = new double[size*size];
        double[] phc = new double[size*size];


        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                pha[i * size + j] = 1.0;

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                phb[i * size + j] = i + 1.0;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < size; i++)

            for (int j = 0; j < size; j++)
            {
                double temp = 0;

                for (int k = 0; k < size; k++)
                {
                    double a = pha[i * size + k];
                    double b = phb[k * size + j];
                    temp += a * b;
                }
                phc[i * size + j] = temp;
            }

        long stopTime = System.currentTimeMillis();
        System.out.println("TIME:" + (stopTime - startTime) + "\n");

        for (int i = 0; i < 1; i++)
        {
            for (int j = 0; j < min(10, size); j++)
                System.out.println(phc[j]);
        }
    }


    public static void lineMult(int size){
        double[] pha = new double[size*size];
        double[] phb = new double[size*size];
        double[] phc = new double[size*size];


        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                pha[i * size + j] = 1.0;

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                phb[i * size + j] = i + 1.0;

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                phc[i * size + j] = 0.0;

        long startTime = System.currentTimeMillis();



        for (int i = 0; i < size; i++)

            for (int j = 0; j < size; j++)

                for (int k = 0; k < size; k++)
                {
                    phc[i * size + k] += pha[i * size + k] * phb[j * size + k];

                }

        long stopTime = System.currentTimeMillis();
        System.out.println("TIME:" + (stopTime - startTime) + "\n");

        for (int i = 0; i < 1; i++)
        {
            for (int j = 0; j < min(10, size); j++)
                System.out.println(phc[j]);
        }
    }


    public static void blockMul(int size, int bkSize){
        double[] pha = new double[size*size];
        double[] phb = new double[size*size];
        double[] phc = new double[size*size];


        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                pha[i * size + j] = 1.0;

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                phb[i * size + j] = i + 1.0;

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                phc[i * size + j] = 0.0;

        long startTime = System.currentTimeMillis();


        for (int ii = 0; ii < (size / bkSize); ii += bkSize)

            for (int jj = 0; jj < (size / bkSize); jj += bkSize)

                for (int kk = 0; kk < (size / bkSize); kk += bkSize)

                    for (int i = 0; i < size; i++)

                                for (int j = 0; j < size; j++)

                                    for (int k = 0; k < size; k++)
                                    {
                                        double a = pha[i * size + k];
                                        double b = phb[j * size + k];
                                double res = a * b + phc[i * size + k];
                                phc[i * size + k] = res;
                            }

        long stopTime = System.currentTimeMillis();
        System.out.println("TIME:" + (stopTime - startTime) + "\n");

        for (int i = 0; i < 1; i++)
        {
            for (int j = 0; j < min(10, size); j++)
                System.out.println(phc[j]);
        }
    }
}
