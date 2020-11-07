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
        self._multipleModels = 0
        self._start = 0
        self._end = -1

    def getParams(self):
        return_args = []
        for key, value in self.__dict__.iteritems():
            paramName = key.replace('_', '-')
            return_args.append(paramName)
            return_args.append(str(value))
        return return_args


class ProfileAPTest(ProfileParam, object):
    def __init__(self):
        super(ProfileAPTest, self).__init__()
        self._inputFile = 'examples/data/ap.json'
        self._modelFile = 'examples/models/model.ap-200.json'
        self._LDASampler = 'binary'
        self._random = 'deterministic'
        self._task = 'test'
        self._multipleModels = 1
        self._start = 1909
        self._end = -1


class ProfileAPTrain(ProfileParam, object):
    def __init__(self):
        super(ProfileAPTrain, self).__init__()
        self._inputFile = 'examples/data/ap.json'
        self._modelFile = 'examples/models/model.ap.json'
        self._LDASampler = 'binary'
        self._LDAHyperOpt = 'none'
        self._random = 'deterministic'
        self._task = 'train'
        self._start = 0
        self._end = 1909


class ProfileAPTrainHyper(ProfileAPTrain, object):
    def __init__(self):
        super(ProfileAPTrainHyper, self).__init__()
        self._modelFile = 'examples/models/model.ap-slice.json'
        self._LDAHyperOpt = 'slice'


class cmdBuilder(object):
    def __init__(self, profile):
        self._profile = profile
        self._other_params = []

    @classmethod
    def execute(cls, all_args):
        fugue = subprocess.Popen(all_args, stdout = subprocess.PIPE, stderr = subprocess.STDOUT)
        while True:
            nextline = fugue.stdout.readline()
            line = nextline.decode('ascii').rstrip()
            if line == '' and fugue.poll() is not None:
                break
            sys.stdout.write(line+'\n')
            sys.stdout.flush()
        output = fugue.communicate()[0]
        exit_code = fugue.returncode
        if (exit_code == 0):
            return output
        return

    def run(self):
        self._other_params.append('java')
        self._other_params.append('-Xmx8192m')
        self._other_params.append('-jar')
        self._other_params.append('build/libs/fugue-topicmodeling-all-0.1.jar')
        all_args = self._other_params
        all_args.extend(self._profile.getParams())
        cmdStr = ' '.join(all_args)
        print(cmdStr)
        cmdBuilder.execute(all_args)


class cmdCompile(cmdBuilder, object):
    def __init__(self):
        return

    def run(self):
        build_cmd = []
        build_cmd.append('gradle')
        build_cmd.append('build')
        print('==================== Build ===================')
        cmdBuilder.execute(build_cmd)
        build_cmd = []
        build_cmd.append('gradle')
        build_cmd.append('fatJar')
        print('==================== Jar ===================')
        cmdBuilder.execute(build_cmd)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='The Fugue Topic Modeling Package')
    parser.add_argument('--profile', help='the profile to launch', default='ProfileAPTrain')
    parser.add_argument('--task', help='the task to perform', default='build')
    args = parser.parse_args()
    get_class = lambda x: globals()[x]
    if args.task is not None and args.task != '':
        if args.task == 'train' or args.task == 'test':
            if args.profile is not None and args.profile != '':
                print(args.profile)
                runProfile = get_class(args.profile)()
                command = cmdBuilder(runProfile)
        elif args.task == 'build':
            command = cmdCompile()
        command.run()
