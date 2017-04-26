/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spine3.server.procman;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Subscribe;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.Assign;
import org.spine3.server.command.EventFactory;
import org.spine3.server.commandbus.CommandDispatcher;
import org.spine3.server.entity.RecordBasedRepository;
import org.spine3.server.entity.RecordBasedRepositoryShould;
import org.spine3.server.event.EventBus;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.StorageFactorySwitch;
import org.spine3.test.EventTests;
import org.spine3.test.Given;
import org.spine3.test.procman.Project;
import org.spine3.test.procman.ProjectId;
import org.spine3.test.procman.Task;
import org.spine3.test.procman.command.AddTask;
import org.spine3.test.procman.command.CreateProject;
import org.spine3.test.procman.command.StartProject;
import org.spine3.test.procman.event.ProjectCreated;
import org.spine3.test.procman.event.ProjectStarted;
import org.spine3.test.procman.event.TaskAdded;
import org.spine3.testdata.Sample;
import org.spine3.testdata.TestBoundedContextFactory;
import org.spine3.testdata.TestEventBusFactory;
import org.spine3.type.CommandClass;
import org.spine3.type.EventClass;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Commands.createCommand;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public class ProcessManagerRepositoryShould
        extends RecordBasedRepositoryShould<ProcessManagerRepositoryShould.TestProcessManager,
                                            ProjectId,
                                            Project> {

    private static final ProjectId ID = Sample.messageOfType(ProjectId.class);

    private static final CommandContext CMD_CONTEXT = createCommandContext();

    private BoundedContext boundedContext;
    private TestProcessManagerRepository repository;
    private EventBus eventBus;

    // Configuration of the test suite
    //---------------------------------

    private StorageFactory storageFactory() {
        return StorageFactorySwitch.getInstance(boundedContext.isMultitenant())
                                   .get();
    }

    @Override
    protected ProjectId createId(int value) {
        return ProjectId.newBuilder()
                        .setId(format("procman-number-%s", value))
                        .build();
    }

    @Override
    protected RecordBasedRepository<ProjectId, TestProcessManager, Project> createRepository() {
        boundedContext = TestBoundedContextFactory.MultiTenant.newBoundedContext();
        final TestProcessManagerRepository repo = new TestProcessManagerRepository(boundedContext);
        repo.initStorage(storageFactory());
        return repo;
    }

    @Override
    protected TestProcessManager createEntity() {
        final ProjectId id = ProjectId.newBuilder()
                                      .setId("123-id")
                                      .build();
        final TestProcessManager result = Given.processManagerOfClass(TestProcessManager.class)
                                    .withId(id)
                                    .build();
        return result;
    }

    @Override
    protected List<TestProcessManager> createEntities(int count) {
        final List<TestProcessManager> procmans = Lists.newArrayList();

        for (int i = 0; i < count; i++) {
            final ProjectId id = createId(i);

            procmans.add(new TestProcessManager(id));
        }
        return procmans;
    }

    @Override
    @Before
    public void setUp() {
        super.setUp();
        eventBus = spy(TestEventBusFactory.create());
        boundedContext = TestBoundedContextFactory.MultiTenant.newBoundedContext(eventBus);

        boundedContext.getCommandBus()
                      .register(new CommandDispatcher() {
                          @Override
                          public Set<CommandClass> getMessageClasses() {
                              return CommandClass.setOf(AddTask.class);
                          }

                          @Override
                          public void dispatch(CommandEnvelope envelope) {
                              // Simply swallow the command. We need this dispatcher for allowing Process Manager
                              // under test to route the AddTask command.
                          }
                      });

        repository = new TestProcessManagerRepository(boundedContext);
        repository.initStorage(storageFactory());
        TestProcessManager.clearMessageDeliveryHistory();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        boundedContext.close();
        super.tearDown();
    }

    /**
     * Creates an instance of {@link Event} for the passed message.
     *
     * <p>Under normal circumstances an event is produced via {@link EventFactory}.
     * Processing of events in a {@link ProcessManagerRepository} is based on event messages
     * and does not need a properly configured {@link EventContext}. That's why this factory method
     * is sufficient for the purpose of this test suite.
     */
    private static Event createEvent(Message eventMessage) {
        return EventTests.createContextlessEvent(eventMessage);
    }

    private void testDispatchEvent(Message eventMessage) {
        final Event event = createEvent(eventMessage);
        repository.dispatch(event);
        assertTrue(TestProcessManager.processed(eventMessage));
    }

    // Tests
    //----------------------------

    @Test
    public void dispatch_event_and_load_manager() {
        testDispatchEvent(projectCreated());
    }

    @Test
    public void dispatch_several_events() {
        testDispatchEvent(projectCreated());
        testDispatchEvent(taskAdded());
        testDispatchEvent(projectStarted());
    }

    @Test
    public void dispatch_command() throws InvocationTargetException {
        testDispatchCommand(addTask());
    }

    @Test
    public void dispatch_several_commands() throws InvocationTargetException {
        testDispatchCommand(createProject());
        testDispatchCommand(addTask());
        testDispatchCommand(startProject());
    }

    private void testDispatchCommand(Message cmdMsg) throws InvocationTargetException {
        final CommandEnvelope cmd = CommandEnvelope.of(createCommand(cmdMsg, CMD_CONTEXT));
        repository.dispatchCommand(cmd);
        assertTrue(TestProcessManager.processed(cmdMsg));
    }

    @Test
    public void dispatch_command_and_post_events() throws InvocationTargetException {
        testDispatchCommand(addTask());

        final ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);

        verify(eventBus, times(1)).post(argumentCaptor.capture());

        final Event event = argumentCaptor.getValue();

        assertNotNull(event);
        final TaskAdded message = unpack(event.getMessage());
        assertEquals(ID, message.getProjectId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_dispatch_unknown_command() throws InvocationTargetException {
        final Int32Value unknownCommand = Int32Value.getDefaultInstance();
        final CommandEnvelope request =
                CommandEnvelope.of(createCommand(unknownCommand,
                                                 CommandContext.getDefaultInstance()));
        repository.dispatchCommand(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_dispatch_unknown_event() {
        final StringValue unknownEventMessage = StringValue.getDefaultInstance();
        final Event event = createEvent(unknownEventMessage);
        repository.dispatch(event);
    }

    @Test
    public void return_command_classes() {
        final Set<CommandClass> commandClasses = repository.getCommandClasses();
        assertTrue(commandClasses.contains(CommandClass.of(CreateProject.class)));
        assertTrue(commandClasses.contains(CommandClass.of(AddTask.class)));
        assertTrue(commandClasses.contains(CommandClass.of(StartProject.class)));
    }

    @Test
    public void return_event_classes() {
        final Set<EventClass> eventClasses = repository.getMessageClasses();
        assertTrue(eventClasses.contains(EventClass.of(ProjectCreated.class)));
        assertTrue(eventClasses.contains(EventClass.of(TaskAdded.class)));
        assertTrue(eventClasses.contains(EventClass.of(ProjectStarted.class)));
    }

    private static CreateProject createProject() {
        return ((CreateProject.Builder) Sample.builderForType(CreateProject.class))
                .setProjectId(ID)
                .build();
    }

    private static StartProject startProject() {
        return ((StartProject.Builder) Sample.builderForType(StartProject.class))
                .setProjectId(ID)
                .build();
    }

    private static AddTask addTask() {
        return ((AddTask.Builder) Sample.builderForType(AddTask.class))
                .setProjectId(ID)
                .build();
    }

    private static ProjectStarted projectStarted() {
        return ((ProjectStarted.Builder) Sample.builderForType(ProjectStarted.class))
                .setProjectId(ID)
                .build();
    }

    private static ProjectCreated projectCreated() {
        return ((ProjectCreated.Builder) Sample.builderForType(ProjectCreated.class))
                .setProjectId(ID)
                .build();
    }

    private static TaskAdded taskAdded() {
        return ((TaskAdded.Builder) Sample.builderForType(TaskAdded.class))
                .setProjectId(ID)
                .build();
    }

    private static class TestProcessManagerRepository
            extends ProcessManagerRepository<ProjectId, TestProcessManager, Project> {

        private TestProcessManagerRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    // Marked as {@code public} to reuse for {@code CommandBus} dispatcher registration tests as well
    // with no code duplication.
    public static class TestProcessManager extends ProcessManager<ProjectId, Project> {

        /** The event message we store for inspecting in delivery tests. */
        private static final Multimap<ProjectId, Message> messagesDelivered = HashMultimap.create();

        @SuppressWarnings(
                {"PublicConstructorInNonPublicClass",           /* A Process Manager constructor must be public
                                                                 * by convention. It is used by reflection
                                                                 * and is part of public API of process managers. */
                        "WeakerAccess"})
        public TestProcessManager(ProjectId id) {
            super(id);
        }

        private void keep(Message commandOrEventMsg) {
            messagesDelivered.put(getState().getId(), commandOrEventMsg);
        }

        private static boolean processed(Message eventMessage) {
            final boolean result = messagesDelivered.containsValue(eventMessage);
            return result;
        }

        static void clearMessageDeliveryHistory() {
            messagesDelivered.clear();
        }

        @SuppressWarnings("UnusedParameters") /* The parameter left to show that a projection subscriber
                                                 can have two parameters. */
        @Subscribe
        public void on(ProjectCreated event, EventContext ignored) {
            // Keep the event message for further inspection in tests.
            keep(event);

            handleProjectCreated(event.getProjectId());
        }

        private void handleProjectCreated(ProjectId projectId) {
            final Project newState = getState().toBuilder()
                                               .setId(projectId)
                                               .setStatus(Project.Status.CREATED)
                                               .build();
            incrementState(newState);
        }

        @Subscribe
        public void on(TaskAdded event) {
            keep(event);

            final Task task = event.getTask();
            handleTaskAdded(task);
        }

        private void handleTaskAdded(Task task) {
            final Project newState = getState().toBuilder()
                                               .addTask(task)
                                               .build();
            incrementState(newState);
        }

        @Subscribe
        public void on(ProjectStarted event) {
            keep(event);

            handleProjectStarted();
        }

        private void handleProjectStarted() {
            final Project newState = getState().toBuilder()
                                               .setStatus(Project.Status.STARTED)
                                               .build();
            incrementState(newState);
        }

        @SuppressWarnings("UnusedParameters") /* The parameter left to show that a command subscriber
                                                 can have two parameters. */
        @Assign
        ProjectCreated handle(CreateProject command, CommandContext ignored) {
            keep(command);

            handleProjectCreated(command.getProjectId());
            final ProjectCreated event = ((ProjectCreated.Builder) Sample.builderForType(ProjectCreated.class))
                    .setProjectId(command.getProjectId())
                    .build();
            return event;
        }

        @SuppressWarnings("UnusedParameters") /* The parameter left to show that a command subscriber
                                                 can have two parameters. */
        @Assign
        TaskAdded handle(AddTask command, CommandContext ignored) {
            keep(command);

            handleTaskAdded(command.getTask());
            final TaskAdded event = ((TaskAdded.Builder) Sample.builderForType(TaskAdded.class))
                    .setProjectId(command.getProjectId())
                    .build();
            return event;
        }

        @Assign
        CommandRouted handle(StartProject command, CommandContext context) {
            keep(command);

            handleProjectStarted();
            final Message addTask = ((AddTask.Builder) Sample.builderForType(AddTask.class))
                    .setProjectId(command.getProjectId())
                    .build();
            return newRouterFor(command, context)
                    .add(addTask)
                    .routeAll();
        }
    }
}
