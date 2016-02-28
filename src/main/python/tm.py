import argparse
import sys
import json
import codecs
import string

def cleaned(raw_word):
    clean_word = []
    for i in raw_word:
        if i in string.ascii_letters:
            clean_word.append(i)
    if len(clean_word) < 2:
        return ''
    return ''.join(clean_word)

def tokenize(raw_string):
    raw_str = raw_string.strip().lower()
    words = []
    for w in raw_str.split(' '):
        c_w = cleaned(w)
        if c_w != '':
            words.append(c_w)
    return words

def parse_docs(dictionary, args):
    input_f = open(args.input_file, 'r')
    output_f = open(args.output_file,'w')
    doc_id = 0
    for line in input_f:
        if line.strip()!='':
            try:
                json_obj = json.loads(line)
                title_raw = json_obj['title']
                review_raw = json_obj['review']
                tokens = []
                tokens.extend(tokenize(title_raw))
                tokens.extend(tokenize(review_raw))
                output_buffer = []
                for t in tokens:
                    if t in dictionary:
                        output_buffer.append({'feature_type' : 'TOKEN', 'feature_name' : t, 'feature_value' : 1.0})
                if len(output_buffer) > 5:
                    output_obj = {}
                    output_obj['doc_id'] = str(doc_id)
                    output_obj['features'] = output_buffer
                    output_f.write(json.dumps(output_obj) + '\n')
                    output_f.flush()
                    doc_id += 1
            except:
                continue
    output_f.close()

def load_raw(args):
    input_f = open(args.input_file, 'r')
    docs = []
    for line in input_f:
        if line.strip()!='':
            try:
                json_obj = json.loads(line)
                title_raw = json_obj['title']
                review_raw = json_obj['review']
                tokens = []
                tokens.extend(tokenize(title_raw))
                tokens.extend(tokenize(review_raw))
                docs.append(tokens)
            except:
                continue
    input_f.close()
    return docs

def compute_term_stats(docs, args = None):
    """
    This function is to compute TF and DF stats
    :param docs:
    :return: a return obj with tf and df
    """
    df = {}
    tf = {}
    for i in range(len(docs)):
        local_df = set()
        for token in docs[i]:
            if not token in local_df:
                local_df.add(token)
                if not token in df:
                    df[token] = 0
                df[token] = df[token] + 1
            if not token in tf:
                tf[token] = 0
            tf[token] = tf[token] + 1
    result_obj = {}
    result_obj['tf'] = tf
    result_obj['df'] = df
    return result_obj

def compute_dictionary(results, args = None):
    """
    This function is to construct the dictionary
    :param results:
    :return:
    """
    tf = results['tf']
    df = results['df']
    tf_list = [(tf_value, tf_term) for (tf_term, tf_value) in tf.iteritems()]
    tf_list = sorted(tf_list, key = lambda s: s[0], reverse = True)
    df_list = [(df_value, df_term) for (df_term, df_value) in df.iteritems()]
    df_list = sorted(df_list, key = lambda s: s[0], reverse = True)
    total = len(df_list)
    upper_bound = int( float(args.term_upper) * total )
    lower_bound = int( float(args.term_lower) * total )
    dictionary = []
    for i in range(upper_bound, lower_bound):
        df_value, df_term = df_list[i]
        if df_value > int(args.term_min):
            dictionary.append((df_term, tf[df_term], df_value))
    return_obj = {}
    return_obj['dictionary'] = dictionary
    return return_obj
    

def save_dictionary(results, args = None):
    """
    This function is to save term results
    :param results: a return obj with term and stats
    :param args: the output filename
    :return:
    """
    output_f = open(args.output_file, 'w')
    dictionary = results['dictionary']
    for t in dictionary:
        term = t[0]
        tf = t[1]
        df = t[2]
        output_f.write(term + '\t' + str(tf) + '\t' + str(df) + '\n')
    output_f.close()

def load_dictionary(args):
    local_dic = set()
    input_f = open(args.dic_file,'r')
    for line in input_f:
        w = line.strip().split('\t')
        local_dic.add(w[0])
    input_f.close()
    return local_dic

def load_model(model_filename):
    json_file_content = ''
    input_f = open(model_filename, 'r')
    for line in input_f:
        json_file_content += line.strip()
    input_f.close()
    model_obj = json.loads(json_file_content)
    alpha = model_obj['alpha']
    topK = len(alpha)
    alphaSum = 0.0
    for a in alpha:
        alphaSum += a
    beta = model_obj['beta']
    betaSum = 0.0
    for b, b_value in beta.iteritems():
        betaSum += b_value
    wordTopicCounts = model_obj['wordTopicCounts']
    topicCounts = model_obj['topicCounts']
    topicsOrder = [{} for i in range(topK)]
    for v in wordTopicCounts.keys():
        local_counts =  wordTopicCounts[v]
        for i in range(len(local_counts)):
            value = (local_counts[i] + beta[v]) / (topicCounts[i] + betaSum)
            topicsOrder[i][v] = value
    return topicsOrder

def load_models(args):
    files = args.model_file.split(',')
    topicsOrders = {}
    N = 0
    for file in files:
        print file
        topicsOrder = load_model(file)
        for k in range(len(topicsOrder)):
            if not k in topicsOrders:
                topicsOrders[k] = {}
            for term, value in topicsOrder[k].iteritems():
                if not term in topicsOrders[k]:
                    topicsOrders[k][term] = 0.0
                topicsOrders[k][term] = topicsOrders[k][term] + value
        N += 1
    # averaging
    topicsK = len(topicsOrders)
    finalOrder = [[] for i in range(topicsK)]
    for k in topicsOrders.keys():
        for term, value in topicsOrders[k].iteritems():
            finalOrder[k].append((value/float(N), term))
    # output
    output_f = open(args.output_file, 'w')
    for k in range(topicsK):
        local_order = sorted(finalOrder[k], key = lambda s: s[0], reverse = True)
        output_buffer = [w for (v, w) in local_order[:40]]
        output_f.write(str(k) + '\t' + ' '.join(output_buffer) + '\n')
    output_f.close()
    return

if __name__ == '__main__':
    sys.stdin = codecs.getreader('utf-8')(sys.stdin)
    sys.stdout = codecs.getwriter('utf8')(sys.stdout)
    parser = argparse.ArgumentParser(description='The prototype ranker for local events.')
    parser.add_argument('--task', help = 'the task to be performed', default = '')
    parser.add_argument('--input_file', help = 'the input filename', default = 'input.db')
    parser.add_argument('--output_file', help = 'the output filename', default = 'output.db')
    parser.add_argument('--dic_file', help = 'the dictionary filename', default = 'dic.db')
    parser.add_argument('--model_file', help = 'the model filename', default = 'model.db')
    parser.add_argument('--term_upper', help = 'the upper bound of term dist.', default = '0.01')
    parser.add_argument('--term_lower', help = 'the lower bound of term dist.', default = '0.80')
    parser.add_argument('--term_min', help = 'the minimum DF value for a term', default = '5')
    args = parser.parse_args()
    if args.task is not None and args.task != '':
        if args.task == 'dictionary':
            docs = load_raw(args)
            results = compute_term_stats(docs, args)
            dictionary = compute_dictionary(results, args)
            save_dictionary(dictionary, args)
        if args.task == 'parse':
            dictionary = load_dictionary(args)
            parse_docs(dictionary, args)
        if args.task == 'topics':
            load_models(args)
