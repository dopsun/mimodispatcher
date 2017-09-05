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

* If A1 has synchronizer for "S001", and A2 has synchronizer of "S002", then A2 could be executed before A1.
* If A1 has synchronizer for "S001", and A2 has synchronizer of "S001" as well, then A2 is guaranteed to be executed after A1 finished  since both have synchronizer "S001".
* If A1 has synchronizer for "S001" and "S002", and A2 has synchronizer of "S001" and "S003", then A2 is guaranteed to be executed after A1 finished since both have synchronizer "S001".
