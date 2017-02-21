package zemberek.embedding.fasttext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Args {
    String input;
    String test;
    String output;
    double lr;
    int lrUpdateRate;
    int dim;
    int ws;
    int epoch;
    int minCount;
    int minCountLabel;
    int neg;
    int wordNgrams;
    loss_name loss;
    model_name model;
    int bucket;
    int minn;
    int maxn;
    int thread;
    double t;
    String label;
    int verbose;
    String pretrainedVectors;

    enum model_name {
        cbow(1), sg(2), sup(3);

        int index;

        model_name(int i) {
            this.index = i;
        }
    }

    enum loss_name {
        hs(1), ns(2), softmax(3);

        int index;

        loss_name(int i) {
            this.index = i;
        }
    }

    Args() {
        lr = 0.05;
        dim = 100;
        ws = 5;
        epoch = 5;
        minCount = 5;
        minCountLabel = 0;
        neg = 5;
        wordNgrams = 1;
        loss = loss_name.ns;
        model = model_name.sg;
        bucket = 2000000;
        minn = 3;
        maxn = 6;
        thread = 4;
        lrUpdateRate = 100;
        t = 1e-4;
        label = "__label__";
        verbose = 2;
        pretrainedVectors = "";
    }


    void save(DataOutputStream out) throws IOException {
        out.writeInt(dim);
        out.writeInt(ws);
        out.writeInt(epoch);
        out.writeInt(minCount);
        out.writeInt(neg);
        out.writeInt(wordNgrams);
        out.writeInt(loss.index);
        out.writeInt(model.index);
        out.writeInt(bucket);
        out.writeInt(minn);
        out.writeInt(maxn);
        out.writeInt(lrUpdateRate);
        out.writeDouble(t);
    }

    static Args load(DataInputStream in) throws IOException {
        Args args = new Args();
        args.dim = in.readInt();
        args.ws = in.readInt();
        args.epoch = in.readInt();
        args.minCount = in.readInt();
        args.neg = in.readInt();
        args.wordNgrams = in.readInt();
        int loss = in.readInt();
        if (loss == loss_name.hs.index) {
            args.loss = loss_name.hs;
        } else if (loss == loss_name.ns.index) {
            args.loss = loss_name.ns;
        } else if (loss == loss_name.softmax.index) {
            args.loss = loss_name.softmax;
        } else throw new IllegalStateException("Unknown loss type.");
        int model = in.readInt();
        if (model == model_name.cbow.index) {
            args.model = model_name.cbow;
        } else if (model == model_name.sg.index) {
            args.model = model_name.sg;
        } else if (model == model_name.sup.index) {
            args.model = model_name.sup;
        } else throw new IllegalStateException("Unknown model type.");
        args.bucket = in.readInt();
        args.minn = in.readInt();
        args.maxn = in.readInt();
        args.lrUpdateRate = in.readInt();
        args.t = in.readDouble();
        return args;
    }

}
