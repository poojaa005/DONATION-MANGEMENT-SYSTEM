# DonateHope – NGO Donation Management Frontend

## 🚀 Setup & Run

### Prerequisites
- Node.js 16+
- Backend running on `http://localhost:8080`

### Installation

```bash
cd ngo-frontend
npm install
npm start
```

App runs at: **http://localhost:3000**

---

## 📁 Project Structure

```
src/
├── components/
│   ├── navbar/          Navbar.js + Navbar.css
│   ├── footer/          Footer.js + Footer.css
│   ├── chatbot/         Chatbot.js + Chatbot.css   (AI chatbot)
│   ├── banner/          UrgentBanner.js + UrgentBanner.css
│   └── cards/           CampaignCard.js + CampaignCard.css
├── pages/
│   ├── home/            Home.js + Home.css
│   ├── campaigns/       Campaigns.js, CampaignDetail.js + CSS
│   ├── donations/       MakeDonation.js, DonationHistory.js + CSS
│   ├── auth/            Login.js, Register.js + Auth.css
│   ├── dashboard/       DonorDashboard.js + CSS
│   ├── admin/           AdminDashboard, ManageCampaigns, ManageUsers,
│   │                    ManageUrgentNeeds, ManageRequests, Reports + CSS
│   ├── volunteer/       VolunteerDashboard.js + CSS
│   ├── ngos/            NGOs.js + NGOs.css
│   └── achievements/    Achievements.js + Achievements.css
├── services/            api.js  (all API calls)
├── context/             AuthContext.js  (JWT auth)
├── App.js               Routes & role-based guards
└── index.css            Global CSS variables & utilities
```

---

## 🔐 Authentication & Roles

| Role       | Access                                      |
|------------|---------------------------------------------|
| Guest      | Home, Campaigns, NGOs, Achievements, Chatbot |
| donor      | + Dashboard, Make Donation, History          |
| volunteer  | + Volunteer Dashboard (tasks)                |
| admin / ngo_admin | + Full Admin Panel, Reports         |

---

## ✨ Features

- **Urgent Needs Banner** – auto-shown on Home & Achievements during the time window set by admin
- **AI Chatbot (HopeBot)** – powered by Claude, answers questions about campaigns, donations etc.
- **Guest browsing** – campaigns, NGOs, achievements visible without login
- **Donation flow** – monetary (with payment method) or goods (with items + pickup scheduling)
- **Admin panel** – manage campaigns, users, urgent needs, approve/reject requests, view reports with charts
- **Volunteer dashboard** – view assigned tasks, update status

---

## ⚙️ Backend API

Backend Spring Boot app should be running on `http://localhost:8080`.

The frontend proxies `/api/*` to the backend via `package.json` proxy setting.

---

## 📝 Notes

- All data shown is **live from the database** – no dummy/hardcoded data
- Email & SMS notifications are triggered by the backend on donation events
- The chatbot uses the Anthropic Claude API (claude-sonnet-4-20250514)
