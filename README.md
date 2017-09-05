# MimoDispatcher
Dispatches tasks from multiple sources into multiple executors with guaranteed order and synchronizations.

# Terms
* Task: unit of work Dispatcher dispatches to Executor to run.
  * Task could have zero, one or more synchronizers.
* Dispatcher: a dedicated thread to dispatch incoming tasks to Executors.
* Executor: threads to execute task.

# Execution order guarantee:
* All tasks with shared synchronizer will be executed with its incoming order.
* Tasks without shared synchronizers, including tasks without synchronizer, can be executed in any order.

For example:

* If task T1 has synchronizer S1, and T2 has synchronizer S2, then T2 could be executed before T1.
* If task T1 has synchronizer S1, and T2 has synchronizer S1 as well, then T2 is guaranteed to be executed after T1 finished  since both have synchronizer "S1".
* If task T1 has synchronizer S1 and S2, and T2 has synchronizer of S1 and S3, then T2 is guaranteed to be executed after T1 finished since both have synchronizer "S1".
