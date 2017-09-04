# MimoDispatcher
Dispatches tasks from multiple sources into multiple executors with guaranteed order and synchronizations.

# Terms
* Task: unit of work Dispatcher dispatches to Executor to run.
** Task could have zero, one or more synchronizers.
* Dispatcher: a dedicated thread to dispatch incoming tasks to Executors.
* Executor: threads to execute task.

Execution order guarantee:
* All tasks with one or more overlapping synchronizer will be executed with its incoming order.
