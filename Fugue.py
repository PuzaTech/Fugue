import argparse
import sys
import subprocess

class ProfileParam(object):
    def __init__(self):
        self._inputFile = 'input.db'
        self._modelFile = 'model.db'
        self._task = 'train'
        self._topics = 100
        self._iters = 1000
        self._topk = 100000
        self._LDASampler = 'binary'
        self._random = 'deterministic'
        self._exp = 0
        self._log = 0
        self._saveModel = 1

    def getParams(self):
        return_args = []
        for key, value in self.__dict__.iteritems():
            paramName = key.replace('_', '-')
            return_args.append(paramName)
            return_args.append(str(value))
        return return_args

class ProfileAP(ProfileParam, object):
    def __init__(self):
        super(ProfileAP, self).__init__()
        self._inputFile = 'examples/data/ap.json'
        self._modelFile = 'examples/models/model.ap.json'
        self._LDASampler = 'binary'
        self._random = 'deterministic'


class cmdBuilder(object):
    def __init__(self, profile):
        self._profile = profile
        self._other_params = []
        self._other_params.append('java')
        self._other_params.append('-Xmx8192m')
        self._other_params.append('-jar')
        self._other_params.append('build/libs/fugue-topicmodeling-all-0.1.jar')

    def run(self):
        all_args = self._other_params
        all_args.extend(self._profile.getParams())
        print ' '.join(all_args)
        fugue = subprocess.Popen(all_args, stdout = subprocess.PIPE, stderr = subprocess.STDOUT)

        while True:
            nextline = fugue.stdout.readline()
            if nextline == '' and fugue.poll() is not None:
                break
            sys.stdout.write(nextline)
            sys.stdout.flush()

        output = fugue.communicate()[0]
        exit_code = fugue.returncode
        if (exit_code == 0):
            return output
        return

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='The Fugue Topic Modeling Package')
    parser.add_argument('--profile', help = 'the profile to launch', default = 'ProfileAP')
    args = parser.parse_args()
    if args.profile is not None and args.profile != '':
        get_class = lambda x: globals()[x]
        runProfile = get_class(args.profile)()
        command = cmdBuilder(runProfile)
        command.run()