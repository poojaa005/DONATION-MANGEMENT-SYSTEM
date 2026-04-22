import React, { useEffect, useState } from 'react';
import {
  addDays,
  endOfMonth,
  endOfWeek,
  format,
  isSameDay,
  isSameMonth,
  isToday,
  parseISO,
  startOfMonth,
  startOfWeek,
  subMonths,
  addMonths,
} from 'date-fns';
import DonationSummaryStrip from '../../components/dashboard/DonationSummaryStrip';
import { volunteerAPI, volunteerTaskAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import './VolunteerDashboard.css';

const statusColors = {
  pending: 'tag-yellow',
  in_progress: 'tag-orange',
  completed: 'tag-green',
  cancelled: 'tag-red',
  assigned: 'tag-orange',
};

const DAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const emoji = (codePoint) => String.fromCodePoint(codePoint);

const formatDate = (value) => (value ? new Date(value).toLocaleDateString('en-IN') : '-');

const getTaskScheduleValue = (task) => task?.pickupRequest?.pickupDate || task?.assignedDate || null;

const getTaskScheduleDate = (task) => {
  const value = getTaskScheduleValue(task);
  return value ? parseISO(value) : null;
};

const getTaskScheduleKey = (task) => {
  const taskDate = getTaskScheduleDate(task);
  return taskDate ? format(taskDate, 'yyyy-MM-dd') : null;
};

const sortTasksBySchedule = (taskList) => [...taskList].sort((left, right) => {
  const leftDate = getTaskScheduleDate(left);
  const rightDate = getTaskScheduleDate(right);

  if (!leftDate && !rightDate) {
    return 0;
  }

  if (!leftDate) {
    return 1;
  }

  if (!rightDate) {
    return -1;
  }

  return leftDate.getTime() - rightDate.getTime();
});

const buildPlaceEmbedUrl = (location) => {
  if (!location) {
    return '';
  }

  if (location.latitude != null && location.longitude != null) {
    return `https://www.google.com/maps?q=${location.latitude},${location.longitude}&z=16&output=embed`;
  }

  const address = location.address || location.addressLabel;
  if (!address) {
    return '';
  }

  return `https://www.google.com/maps?q=${encodeURIComponent(address)}&output=embed`;
};

const buildAddressRoute = (fromLat, fromLng, destinationAddress) => {
  const encodedDestination = encodeURIComponent(destinationAddress);

  return {
    googleMapsUrl: `https://www.google.com/maps/dir/?api=1&origin=${fromLat},${fromLng}&destination=${encodedDestination}&travelmode=driving`,
    embedUrl: `https://www.google.com/maps?output=embed&saddr=${fromLat},${fromLng}&daddr=${encodedDestination}`,
  };
};

const VolunteerDashboard = () => {
  const { user } = useAuth();
  const [volunteer, setVolunteer] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [availableTasks, setAvailableTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [acceptingPickupId, setAcceptingPickupId] = useState(null);
  const [taskLocations, setTaskLocations] = useState({});
  const [taskRoutes, setTaskRoutes] = useState({});
  const [mapLoadingTaskId, setMapLoadingTaskId] = useState(null);
  const [routeLoadingTaskId, setRouteLoadingTaskId] = useState(null);
  const [taskViewMode, setTaskViewMode] = useState('normal');
  const [calendarMonth, setCalendarMonth] = useState(new Date());
  const [selectedScheduleDate, setSelectedScheduleDate] = useState(format(new Date(), 'yyyy-MM-dd'));

  useEffect(() => {
    if (!user?.email) {
      setLoading(false);
      return;
    }

    const loadDashboard = async () => {
      try {
        if (!user?.userId) {
          toast.error('User profile not found.');
          return;
        }

        const volunteerResponse = await volunteerAPI.getByUser(user.userId);

        const volunteerData = volunteerResponse.data;
        setVolunteer(volunteerData);

        const [myTasksResponse, availableTasksResponse] = await Promise.all([
          volunteerTaskAPI.getMyTasks(volunteerData.volunteerId),
          volunteerTaskAPI.getAvailablePickups(),
        ]);

        setTasks(myTasksResponse.data || []);
        setAvailableTasks(availableTasksResponse.data || []);
      } catch (error) {
        toast.error(error?.response?.data?.error || 'Failed to load volunteer dashboard.');
      } finally {
        setLoading(false);
      }
    };

    loadDashboard();
  }, [user]);

  useEffect(() => {
    const scheduledTasks = sortTasksBySchedule(tasks);
    const firstTaskDate = scheduledTasks.length > 0 ? getTaskScheduleDate(scheduledTasks[0]) : null;
    const selectedTaskExists = tasks.some((task) => getTaskScheduleKey(task) === selectedScheduleDate);

    if (!selectedTaskExists && firstTaskDate) {
      setSelectedScheduleDate(format(firstTaskDate, 'yyyy-MM-dd'));
      setCalendarMonth(firstTaskDate);
    }
  }, [tasks, selectedScheduleDate]);

  const handleUpdateStatus = async (taskId, newStatus) => {
    try {
      await volunteerTaskAPI.updateStatus(taskId, newStatus);
      setTasks((current) =>
        current.map((task) => (task.taskId === taskId ? { ...task, taskStatus: newStatus } : task))
      );
      toast.success(`Task marked as ${newStatus.replace('_', ' ')}.`);
    } catch {
      toast.error('Failed to update task status.');
    }
  };

  const handleAcceptTask = async (pickupId) => {
    if (!volunteer?.volunteerId) {
      toast.error('Volunteer profile not found.');
      return;
    }

    try {
      setAcceptingPickupId(pickupId);
      const response = await volunteerTaskAPI.acceptTask({
        pickupId,
        volunteerId: volunteer.volunteerId,
      });

      setTasks((current) => [...current, response.data]);
      setAvailableTasks((current) => current.filter((task) => task.pickupId !== pickupId));
      toast.success('Task selected successfully.');
    } catch (error) {
      toast.error(error?.response?.data?.error || 'Failed to select task.');
    } finally {
      setAcceptingPickupId(null);
    }
  };

  const handleLoadTaskMap = async (taskId) => {
    if (taskLocations[taskId]) {
      return;
    }

    try {
      setMapLoadingTaskId(taskId);
      const response = await volunteerTaskAPI.getTaskMap(taskId);
      setTaskLocations((current) => ({ ...current, [taskId]: response.data }));
    } catch (error) {
      const task = tasks.find((entry) => entry.taskId === taskId);
      const fallbackAddress = task?.pickupRequest?.donorAddress;

      if (fallbackAddress) {
        setTaskLocations((current) => ({
          ...current,
          [taskId]: {
            address: fallbackAddress,
            addressLabel: fallbackAddress,
            landmark: '',
            latitude: null,
            longitude: null,
          },
        }));
        toast.info('Exact donor coordinates are missing. Showing the pickup address on the map.');
      } else {
        toast.error(error?.response?.data?.error || 'Donor location is not available for this task.');
      }
    } finally {
      setMapLoadingTaskId(null);
    }
  };

  const handleTrackRoute = async (taskId) => {
    if (!navigator.geolocation) {
      toast.error('Geolocation is not supported in this browser.');
      return;
    }

    setRouteLoadingTaskId(taskId);
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          await handleLoadTaskMap(taskId);
          const response = await volunteerTaskAPI.navigateToTask(
            taskId,
            position.coords.latitude,
            position.coords.longitude
          );
          setTaskRoutes((current) => ({ ...current, [taskId]: response.data }));
          toast.success('Route loaded. Follow the map to reach the donor.');
        } catch (error) {
          const task = tasks.find((entry) => entry.taskId === taskId);
          const fallbackAddress = task?.pickupRequest?.donorAddress;

          if (fallbackAddress) {
            setTaskRoutes((current) => ({
              ...current,
              [taskId]: buildAddressRoute(
                position.coords.latitude,
                position.coords.longitude,
                fallbackAddress
              ),
            }));
            toast.info('Exact donor coordinates are missing. Routing to the pickup address instead.');
          } else {
            toast.error(error?.response?.data?.error || 'Unable to load the route.');
          }
        } finally {
          setRouteLoadingTaskId(null);
        }
      },
      () => {
        toast.error('Unable to access your current location.');
        setRouteLoadingTaskId(null);
      },
      { enableHighAccuracy: true, timeout: 15000 }
    );
  };

  const taskStats = {
    total: tasks.length,
    pending: tasks.filter((task) => task.taskStatus === 'pending').length,
    inProgress: tasks.filter((task) => task.taskStatus === 'in_progress').length,
    completed: tasks.filter((task) => task.taskStatus === 'completed').length,
  };
  const volunteerSummaryItems = [
    {
      label: 'Tasks',
      value: taskStats.total,
      note: 'All pickup tasks assigned to you',
      emoji: emoji(0x1F4CB),
      tone: 'orange',
    },
    {
      label: 'Pending',
      value: taskStats.pending,
      note: 'Tasks waiting for your action',
      emoji: emoji(0x23F3),
      tone: 'yellow',
    },
    {
      label: 'In Progress',
      value: taskStats.inProgress,
      note: 'Pickups currently being handled',
      emoji: emoji(0x1F69A),
      tone: 'teal',
    },
    {
      label: 'Completed',
      value: taskStats.completed,
      note: 'Tasks you already closed successfully',
      emoji: emoji(0x2705),
      tone: 'green',
    },
  ];

  const scheduledTasks = sortTasksBySchedule(tasks);
  const tasksByDay = scheduledTasks.reduce((accumulator, task) => {
    const taskKey = getTaskScheduleKey(task);
    if (!taskKey) {
      return accumulator;
    }

    if (!accumulator[taskKey]) {
      accumulator[taskKey] = [];
    }

    accumulator[taskKey].push(task);
    return accumulator;
  }, {});

  const calendarStart = startOfWeek(startOfMonth(calendarMonth), { weekStartsOn: 1 });
  const calendarEnd = endOfWeek(endOfMonth(calendarMonth), { weekStartsOn: 1 });
  const calendarDays = [];

  for (let current = calendarStart; current <= calendarEnd; current = addDays(current, 1)) {
    calendarDays.push(current);
  }

  const selectedDayDate = parseISO(selectedScheduleDate);
  const selectedDayTasks = tasksByDay[selectedScheduleDate] || [];

  return (
    <div className="volunteer-dashboard page-wrapper">
      <div className="container">
        <div className="dashboard-header">
          <div>
            <h1>Volunteer Dashboard</h1>
            <p>Choose a task, see the donor location, and track the route to collect the donation for the assigned organization.</p>
          </div>
        </div>

        <DonationSummaryStrip items={volunteerSummaryItems} ariaLabel="Volunteer dashboard summary" />

        <div className="vol-tasks">
          <h3>Available Volunteer Tasks</h3>
          {loading ? (
            <div className="loading-spinner"><div className="spinner"></div></div>
          ) : availableTasks.length === 0 ? (
            <div className="empty-state">
              <div style={{ fontSize: '1.4rem', marginBottom: 16, fontWeight: 700 }}>Available Tasks</div>
              <h3>No open volunteer tasks</h3>
              <p>New donor pickup requests will appear here.</p>
            </div>
          ) : (
            <div className="task-cards">
              {availableTasks.map((task) => (
                <div key={task.pickupId} className="task-card card">
                  <div className="task-card-header">
                    <div>
                      <h4>Pickup #{task.pickupId}</h4>
                      <span className="task-pickup">{task.ngoName || 'Organization not assigned'}</span>
                    </div>
                    <span className={`tag ${statusColors[task.status] || 'tag-yellow'}`}>{task.status}</span>
                  </div>
                  <div className="task-details">
                    <div className="td-row"><span>Location</span><strong>{task.address}</strong></div>
                    {task.addressLabel && task.addressLabel !== task.address && (
                      <div className="td-row"><span>Shared GPS label</span><strong>{task.addressLabel}</strong></div>
                    )}
                    {task.landmark && <div className="td-row"><span>Landmark</span><strong>{task.landmark}</strong></div>}
                    <div className="td-row"><span>Donor</span><strong>{task.donorName || 'Donor'}</strong></div>
                    {task.donorPhone && <div className="td-row"><span>Phone</span><strong>{task.donorPhone}</strong></div>}
                    <div className="td-row"><span>Organization</span><strong>{task.ngoName || 'Direct donation'}</strong></div>
                    <div className="td-row"><span>Date</span><strong>{formatDate(task.pickupDate)}</strong></div>
                    {task.timeSlot && <div className="td-row"><span>Time</span><strong>{task.timeSlot}</strong></div>}
                  </div>
                  <div className="task-actions">
                    <button
                      className="act-btn act-teal"
                      onClick={() => handleAcceptTask(task.pickupId)}
                      disabled={acceptingPickupId === task.pickupId}
                    >
                      {acceptingPickupId === task.pickupId ? 'Selecting...' : 'Select This Task'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="vol-section-head section-spacer">
            <div>
              <h3>My Selected Tasks</h3>
              <p className="vol-section-note">Switch between the full task cards and a month-based schedule view.</p>
            </div>
            <div className="vol-view-toggle" role="tablist" aria-label="Volunteer task views">
              <button
                type="button"
                className={`vol-view-btn ${taskViewMode === 'normal' ? 'active' : ''}`}
                onClick={() => setTaskViewMode('normal')}
              >
                Normal View
              </button>
              <button
                type="button"
                className={`vol-view-btn ${taskViewMode === 'calendar' ? 'active' : ''}`}
                onClick={() => setTaskViewMode('calendar')}
              >
                Calendar View
              </button>
            </div>
          </div>

          {loading ? (
            <div className="loading-spinner"><div className="spinner"></div></div>
          ) : tasks.length === 0 ? (
            <div className="empty-state">
              <div style={{ fontSize: '1.4rem', marginBottom: 16, fontWeight: 700 }}>No Tasks</div>
              <h3>No tasks selected yet</h3>
              <p>Select a donor pickup above to see the route and assigned organization here.</p>
            </div>
          ) : taskViewMode === 'calendar' ? (
            <div className="vol-calendar-shell">
              <div className="vol-calendar card">
                <div className="vol-calendar-head">
                  <div>
                    <h4>{format(calendarMonth, 'MMMM yyyy')}</h4>
                    <p>{scheduledTasks.length} scheduled task(s)</p>
                  </div>
                  <div className="vol-calendar-nav">
                    <button type="button" className="btn-secondary" onClick={() => setCalendarMonth((current) => subMonths(current, 1))}>
                      Prev
                    </button>
                    <button type="button" className="btn-secondary" onClick={() => setCalendarMonth(new Date())}>
                      Today
                    </button>
                    <button type="button" className="btn-secondary" onClick={() => setCalendarMonth((current) => addMonths(current, 1))}>
                      Next
                    </button>
                  </div>
                </div>

                <div className="vol-calendar-weekdays">
                  {DAY_LABELS.map((label) => (
                    <span key={label}>{label}</span>
                  ))}
                </div>

                <div className="vol-calendar-grid">
                  {calendarDays.map((day) => {
                    const dayKey = format(day, 'yyyy-MM-dd');
                    const dayTasks = tasksByDay[dayKey] || [];

                    return (
                      <button
                        key={dayKey}
                        type="button"
                        className={[
                          'vol-calendar-cell',
                          isSameMonth(day, calendarMonth) ? '' : 'muted',
                          isToday(day) ? 'today' : '',
                          isSameDay(day, selectedDayDate) ? 'selected' : '',
                        ].filter(Boolean).join(' ')}
                        onClick={() => {
                          setSelectedScheduleDate(dayKey);
                          setCalendarMonth(day);
                        }}
                      >
                        <div className="vol-calendar-date">
                          <span>{format(day, 'd')}</span>
                          {dayTasks.length > 0 && (
                            <strong>{dayTasks.length} task{dayTasks.length > 1 ? 's' : ''}</strong>
                          )}
                        </div>
                        <div className="vol-calendar-events">
                          {dayTasks.slice(0, 3).map((task) => (
                            <div key={task.taskId} className={`vol-calendar-pill ${task.taskStatus}`}>
                              <span>#{task.pickupRequest?.pickupId || task.taskId}</span>
                              <span>{task.pickupRequest?.timeSlot || 'Flexible'}</span>
                            </div>
                          ))}
                          {dayTasks.length > 3 && (
                            <div className="vol-calendar-more">+{dayTasks.length - 3} more</div>
                          )}
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="vol-agenda card">
                <div className="vol-agenda-head">
                  <div>
                    <h4>{format(selectedDayDate, 'dd MMMM yyyy')}</h4>
                    <p>{selectedDayTasks.length} scheduled task(s)</p>
                  </div>
                </div>

                {selectedDayTasks.length === 0 ? (
                  <div className="empty-state">
                    <div style={{ fontSize: '1.2rem', marginBottom: 12, fontWeight: 700 }}>No Schedule</div>
                    <p>No volunteer tasks are scheduled for this date.</p>
                  </div>
                ) : (
                  <div className="vol-agenda-list">
                    {selectedDayTasks.map((task) => {
                      const pickup = task.pickupRequest;
                      const donation = pickup?.donation;
                      const ngo = donation?.campaign?.ngo || donation?.ngo;

                      return (
                        <div key={task.taskId} className="vol-agenda-item">
                          <div className="vol-agenda-top">
                            <div>
                              <h5>Pickup #{pickup?.pickupId || '-'}</h5>
                              <span>{pickup?.timeSlot || 'Flexible timing'}</span>
                            </div>
                            <span className={`tag ${statusColors[task.taskStatus] || 'tag-yellow'}`}>
                              {task.taskStatus?.replace('_', ' ')}
                            </span>
                          </div>
                          <div className="vol-agenda-meta">
                            <span>{ngo?.ngoName || 'Direct donation'}</span>
                            <span>{pickup?.donorAddress || '-'}</span>
                            <span>{donation?.user?.name || 'Donor'}</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="task-cards">
              {tasks.map((task) => {
                const pickup = task.pickupRequest;
                const donation = pickup?.donation;
                const ngo = donation?.campaign?.ngo || donation?.ngo;
                const taskLocation = taskLocations[task.taskId];
                const taskRoute = taskRoutes[task.taskId];
                const taskLocationUrl = buildPlaceEmbedUrl(taskLocation);
                const canShowNavigation = task.taskStatus !== 'completed' && task.taskStatus !== 'cancelled';

                return (
                  <div key={task.taskId} className="task-card card">
                    <div className="task-card-header">
                      <div>
                        <h4>Task #{task.taskId}</h4>
                        <span className="task-pickup">Pickup #{pickup?.pickupId || '-'}</span>
                      </div>
                      <span className={`tag ${statusColors[task.taskStatus] || 'tag-yellow'}`}>
                        {task.taskStatus?.replace('_', ' ')}
                      </span>
                    </div>
                    {pickup && (
                      <div className="task-details">
                        <div className="td-row"><span>Location</span><strong>{pickup.donorAddress}</strong></div>
                        {pickup.donorPhone && <div className="td-row"><span>Phone</span><strong>{pickup.donorPhone}</strong></div>}
                        <div className="td-row"><span>Donor</span><strong>{donation?.user?.name || 'Donor'}</strong></div>
                        <div className="td-row"><span>Campaign</span><strong>{donation?.campaign?.title || 'Direct donation'}</strong></div>
                        <div className="td-row"><span>Organization</span><strong>{ngo?.ngoName || volunteer?.ngo?.ngoName || 'Direct donation'}</strong></div>
                        <div className="td-row"><span>Date</span><strong>{formatDate(pickup.pickupDate)}</strong></div>
                        {pickup.timeSlot && <div className="td-row"><span>Time</span><strong>{pickup.timeSlot}</strong></div>}
                        <div className="td-row"><span>Status</span><span className="tag tag-orange">{pickup.pickupStatus}</span></div>
                      </div>
                    )}

                    <div className="task-actions task-actions-wrap">
                      {canShowNavigation && (
                        <button
                          className="act-btn act-blue"
                          onClick={() => handleLoadTaskMap(task.taskId)}
                          disabled={mapLoadingTaskId === task.taskId}
                        >
                          {mapLoadingTaskId === task.taskId ? 'Loading Donor Map...' : 'Show Donor Map'}
                        </button>
                      )}
                      {canShowNavigation && (
                        <button
                          className="act-btn act-orange"
                          onClick={() => handleTrackRoute(task.taskId)}
                          disabled={routeLoadingTaskId === task.taskId}
                        >
                          {routeLoadingTaskId === task.taskId ? 'Loading Route...' : 'Track Route'}
                        </button>
                      )}
                      {task.taskStatus === 'pending' && (
                        <button className="act-btn act-teal" onClick={() => handleUpdateStatus(task.taskId, 'in_progress')}>
                          Start Task
                        </button>
                      )}
                      {task.taskStatus === 'in_progress' && (
                        <button className="act-btn act-green" onClick={() => handleUpdateStatus(task.taskId, 'completed')}>
                          Mark Complete
                        </button>
                      )}
                    </div>

                    {canShowNavigation && taskLocation && (
                      <div className="task-map-card">
                        <div className="task-map-header">
                          <strong>Donor location</strong>
                          {taskLocation.landmark && <span>{taskLocation.landmark}</span>}
                        </div>
                        {taskLocationUrl && (
                          <iframe
                            title={`Donor map ${task.taskId}`}
                            className="task-map-frame"
                            src={taskLocationUrl}
                            loading="lazy"
                            allowFullScreen
                          />
                        )}
                      </div>
                    )}

                    {canShowNavigation && taskRoute && (
                      <div className="task-map-card">
                        <div className="task-map-header">
                          <strong>Route to donor</strong>
                          <a href={taskRoute.googleMapsUrl} target="_blank" rel="noreferrer">Open in Google Maps</a>
                        </div>
                        <iframe
                          title={`Route map ${task.taskId}`}
                          className="task-map-frame"
                          src={taskRoute.embedUrl}
                          loading="lazy"
                          allowFullScreen
                        />
                      </div>
                    )}

                    <div className="task-assigned">Assigned: {formatDate(task.assignedDate)}</div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default VolunteerDashboard;
